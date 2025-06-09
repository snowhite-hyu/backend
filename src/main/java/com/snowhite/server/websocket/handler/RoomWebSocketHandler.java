package com.snowhite.server.websocket.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snowhite.server.domain.session.Room;
import com.snowhite.server.domain.entity.User;
import com.snowhite.server.payload.WsMessage;
import com.snowhite.server.repository.UserRepository;
import com.snowhite.server.security.jwt.JwtProvider;
import com.snowhite.server.service.RoomService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomWebSocketHandler implements WebSocketHandler {

    private static final String ROOM_SESSION_PREFIX = "room_session:";

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplateForSessionIds;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    private final RoomService roomService;

    private final ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    @Override
    @NonNull
    public Mono<Void> handle(WebSocketSession session) {

        String uri = session.getHandshakeInfo().getUri().toString();
        String token;

        try {
            token = jwtProvider.extractTokenFromURI(uri);
        } catch (Exception e) {
            return sendMessage(session, "error", "Invalid URI format" + uri).then();
        }

        if (token == null) {
            return sendMessage(session, "error", "Missing token").then();
        }

        if (!jwtProvider.isTokenValid(token)) {
            return sendMessage(session, "error", "Invalid token").then();
        }

        long userId = jwtProvider.extractUserIdFromToken(token);

        return reactiveRedisTemplateForSessionIds.opsForValue().set(ROOM_SESSION_PREFIX + String.valueOf(userId), session.getId())
                .doOnSuccess(ignored -> sessionMap.put(session.getId(), session))
                .onErrorResume(throwable -> sendMessage(session, "error", "Failed to save session").thenReturn(true))
                .then(
                        session.receive()
                                .doFinally(signalType -> sessionMap.remove(session.getId()))
                                .map(WebSocketMessage::getPayloadAsText)
                                .flatMap(payload -> processMessage(session, payload, token))
                                .onErrorResume(throwable -> sendMessage(session, "error", throwable.getMessage()).then())
                                .then()
                );
    }

    private Mono<Void> processMessage(WebSocketSession session, String payload, String token) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String action = root.get("type").asText().toLowerCase();
            JsonNode node = root.get("payload");

            switch (action) {
                case "create":
                {
                    long userId = jwtProvider.extractUserIdFromToken(token);
                    int capacity = node.get("capacity").asInt();
                    int turnTime = node.get("turnTime").asInt();
                    String roomName = node.get("roomName").asText();

                    return handleCreateRoom(session, userId, capacity, turnTime, roomName);
                }

                case "join":
                {
                    long userId = jwtProvider.extractUserIdFromToken(token);
                    Long roomId = Long.parseLong(node.get("roomId").asText());
                    return handleJoinRoom(session, userId, roomId);
                }

                case "quit":
                {
                    long userId = jwtProvider.extractUserIdFromToken(token);
                    Long roomId = Long.parseLong(node.get("roomId").asText());
                    return handleQuitRoom(session, userId, roomId);
                }

                case "start-game":
                {
                    long roomId = Long.parseLong(node.get("roomId").asText());
                    return handleStartGame(session, roomId);
                }

                case "chat": {
                    long userId = jwtProvider.extractUserIdFromToken(token);
                    Long roomId = Long.parseLong(node.get("roomId").asText());
                    String message = node.get("message").asText();

                    return handleChatMessage(session, userId, roomId, message);
                }

                default:
                {
                    return sendMessage(session, "error", "unsupported action: " + action);
                }
            }

        } catch (Exception e) {
            return sendMessage(session, "error", "Invalid Websocket frame");
        }
    }

    private Mono<Void> handleStartGame(WebSocketSession session, long roomId) {

        return roomService.startGameByRoomId(roomId)
                .flatMap(result -> broadcastMessageToRoom(roomId, "Game-Started", result));
    }

    private Mono<Void> handleCreateRoom(WebSocketSession session, long userId, int capacity, int turnTime, String roomName) {

        return roomService.createRoom(userId, roomName, capacity, turnTime)
                .flatMap(result -> {
                    if (result.isSuccess()) {
                        return sendMessage(session, "created-room", result.getRoom());
                    } else {
                        return sendMessage(session, "error", result.getErrorMessage());
                    }
       
                });
    }

    private Mono<Void> handleJoinRoom(WebSocketSession session, long userId, Long roomId) {
        return roomService.joinRoom(userId, roomId)
                .flatMap(result -> {
                    if (result.isSuccess()) {
                        return broadcastToRoom(result.getRoom().getUsers().stream().filter(u -> u.getId() != userId), "room-users", result.getRoom().getUsers())
                                .then(sendMessage(session, "joined-room", result.getRoom()));
                    } else {
                        return sendMessage(session, "error", result.getErrorMessage());
                    }
                });
    }

    private Mono<Void> handleQuitRoom(WebSocketSession session, long userId, Long roomId) {
        return roomService.quitRoom(userId, roomId, session.getId())
                .flatMap(result -> {
                    if (!result.isSuccess()) {
                        return sendMessage(session, "error", result.getErrorMessage());
                    }

                    Room room = result.getRoom();
                    boolean isMaster = room.getMasterPlayer().getId() == userId;

                    if (isMaster) {
                        // 1. 모든 유저에게 quit-success
                        Mono<Void> broadcast = broadcastToRoom(room.getUsers().stream(), "quit-success", null);
                        // 2. broadcast 후, 모든 유저의 세션/redis 삭제
                        Mono<Void> cleanup = Mono.when(
                                room.getUsers().stream().map(user ->
                                        reactiveRedisTemplateForSessionIds.opsForValue().get(ROOM_SESSION_PREFIX + user.getId())
                                                .flatMap(sid -> {
                                                    Mono<Boolean> redisDel = reactiveRedisTemplateForSessionIds.delete(ROOM_SESSION_PREFIX + user.getId()).thenReturn(Boolean.TRUE);
                                                    Mono<Void> close = Mono.fromRunnable(() -> {
                                                        if (sid != null) {
                                                            WebSocketSession ws = sessionMap.remove(sid);
                                                            if (ws != null && ws.isOpen()) {
                                                                ws.close().subscribe();
                                                            }
                                                        }
                                                    });
                                                    return redisDel.then(close);
                                                })
                                ).collect(Collectors.toList())
                        );
                        return broadcast.then(cleanup);
                    } else {
                        // 1. 본인에게 quit-success
                        Mono<Void> toSelf = sendMessage(session, "quit-success", null);
                        Mono<Void> broadcast = broadcastToRoom(room.getUsers().stream(), "room-users", room.getUsers());
                        // 2. 메시지 전송 후, 본인 세션/redis 삭제
                        Mono<Boolean> redisDel = reactiveRedisTemplateForSessionIds.delete(ROOM_SESSION_PREFIX + userId).thenReturn(Boolean.TRUE);
                        Mono<Void> close = Mono.fromRunnable(() -> {
                            sessionMap.remove(session.getId());
                            if (session.isOpen()) {
                                session.close().subscribe();
                            }
                        });
                        return toSelf.then(broadcast).then(redisDel).then(close);
                    }
                });
    }

    private Mono<Void> broadcastToRoom(Stream<User> users, String type, Object payload) {
        List<Mono<Void>> broadcasts = users
                .map(user -> reactiveRedisTemplateForSessionIds.opsForValue().get(ROOM_SESSION_PREFIX + user.getId())
                        .flatMap(sessionId -> {
                            WebSocketSession userSession = sessionMap.get(sessionId);
                            if (userSession != null && userSession.isOpen()) {
                                return sendMessage(userSession, type, payload);
                            } else {
                                return Mono.empty();
                            }
                        }))
                .collect(Collectors.toList());
        return Flux.concat(broadcasts).then();
    }

    private Mono<Void> sendMessage(WebSocketSession session, String type, Object payload) {

        try {
            String json = objectMapper.writeValueAsString(
                    WsMessage.onSuccess(type, payload)
            );
            return session.send(Mono.just(
                    session.textMessage(json)
            ));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Mono.error(e);
        }
    }

    public Mono<Void> broadcastMessageToRoom(Long roomId, String type, Object payload) {

        return roomService.getRoomByRoomId(roomId)
                .flatMapMany(room -> Flux.fromIterable(room.getUsers()))
                .flatMap(user -> {
                    Long userId = user.getId();
                    return reactiveRedisTemplateForSessionIds.opsForValue().get(ROOM_SESSION_PREFIX + userId)
                            .flatMap(sessionId -> {
                                WebSocketSession sessionToSend = sessionMap.get(sessionId);
                                return sendMessage(sessionToSend, type, payload);
                            });
                })
                .then();
    }

    private Mono<Void> handleChatMessage(WebSocketSession session, long userId, Long roomId, String message) {

        return roomService.getRoomByRoomId(roomId)
                .flatMap(room -> {
                    User sender = room.getUsers().stream()
                            .filter(u -> u.getId() == userId)
                            .findFirst()
                            .orElse(null);

                    if (sender == null) {
                        return sendMessage(session, "error", "User is not in room");
                    }

                    // payload: { user: User, message: String }
                    com.fasterxml.jackson.databind.node.ObjectNode chatPayload = objectMapper.createObjectNode();
                    chatPayload.set("user", objectMapper.valueToTree(sender));
                    chatPayload.put("message", message);
                    return broadcastMessageToRoom(roomId, "chat", chatPayload);
                })
                .switchIfEmpty(sendMessage(session, "error", "Room is not found"));
    }

}