package com.snowhite.server.websocket.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snowhite.server.domain.session.Room;
import com.snowhite.server.domain.entity.User;
import com.snowhite.server.repository.UserRepository;
import com.snowhite.server.security.jwt.JwtProvider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RoomWebSocketHandler implements WebSocketHandler {

    private static final AtomicLong roomIdGenerator = new AtomicLong(0);

    private final ReactiveRedisTemplate<String, Room> redisTemplateForRooms;
    private final ReactiveRedisTemplate<Long, String> redisTemplateForSessionIds;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    private static final String ROOM_PREFIX = "room:";

    public RoomWebSocketHandler(
            @Qualifier("reactiveRedisTemplateForRooms")
            ReactiveRedisTemplate<String, Room> redisTemplateForRooms,
            @Qualifier("reactiveRedisTemplateForSessionIds")
            ReactiveRedisTemplate<Long, String> redisTemplateForSessionIds,
            UserRepository userRepository,
            JwtProvider jwtProvider,
            ObjectMapper objectMapper
    ) {
        this.redisTemplateForRooms = redisTemplateForRooms;
        this.redisTemplateForSessionIds = redisTemplateForSessionIds;
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.objectMapper = objectMapper;
    }

    private String extractTokenFromUri(String uri) {
        try {
            URI parsedUri = new URI(uri);
            String query = parsedUri.getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && pair[0].equals("token")) {
                        return pair[1];
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Mono<Void> broadcastToRoom(Room room, String message) {
        List<Mono<Void>> broadcasts = room.getUsers().stream()
                .map(user -> redisTemplateForSessionIds.opsForValue().get(user.getId())
                        .flatMap(sessionId -> {
                            WebSocketSession userSession = sessionMap.get(sessionId);
                            if (userSession != null && userSession.isOpen()) {
                                return userSession.send(Mono.just(userSession.textMessage(message)));
                            } else {
                                return Mono.empty();
                            }
                        }))
                .collect(Collectors.toList());

        return Flux.concat(broadcasts).then();
    }

    @Override
    @NonNull
    public Mono<Void> handle(WebSocketSession session) {

        String uri = session.getHandshakeInfo().getUri().toString();
        String token;

        try {
            token = extractTokenFromUri(uri);
        } catch (Exception e) {
            return session.send(Mono.just(session.textMessage("Invalid URI format"))).then();
        }

        if (token == null) {
            return session.send(Mono.just(session.textMessage("Missing token"))).then();
        }

        if (!jwtProvider.isTokenValid(token)) {
            return session.send(Mono.just(session.textMessage("Invalid token"))).then();
        }

        long userId = jwtProvider.extractUserIdFromToken(token);

        return redisTemplateForSessionIds.opsForValue().set(userId, session.getId())
                .doOnSuccess(ignored -> sessionMap.put(session.getId(), session))
                .then(
                        session.receive()
                                .doFinally(signalType -> sessionMap.remove(session.getId()))
                                .map(WebSocketMessage::getPayloadAsText)
                                .flatMap(processMessage(session, token))
                                .then()
                );
    }

    private Function<String, Publisher<? extends Void>> processMessage(WebSocketSession session, String token) {
        return payload -> {
            try {
                JsonNode node = objectMapper.readTree(payload);
                String action = node.get("action").asText().toLowerCase();

                switch (action) {
                    case "create":
                    {
                        long userId = jwtProvider.extractUserIdFromToken(token);
                        int capacity = node.get("capacity").asInt();
                        int turnTime = node.get("turnTime").asInt();

                        return handleCreateRoom(session, userId, capacity, turnTime);
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
                        String roomId = node.get("roomId").asText();
                        return handleQuitRoom(session, userId, roomId);
                    }

                    default:
                    {
                        return session.send(Mono.just(
                                session.textMessage("Unsupported action: " + action)));
                    }
                }

            } catch (Exception e) {
                return session.send(Mono.just(
                        session.textMessage("Invalid frame: " + e.getMessage())));
            }
        };
    }

    private Mono<Void> handleCreateRoom(WebSocketSession session, long userId, int capacity, int turnTime) {

        return Mono.fromCallable(() -> userRepository.findById(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optionalUser -> {
                    if (optionalUser.isEmpty()) {
                        return session.send(Mono.just(
                                session.textMessage("User not found")));
                    }

                    User user = optionalUser.get();
                    Long roomId = roomIdGenerator.incrementAndGet();
                    List<User> users = new ArrayList<>();
                    users.add(user);

                    Room room = new Room(roomId, user, users, capacity, turnTime, false);

                    Mono<Boolean> saveRoom = redisTemplateForRooms.
                            opsForValue().set(ROOM_PREFIX + String.valueOf(roomId), room);

                    try {
                        return Mono.when(saveRoom)
                                .then(session.send(Mono.just(
                                        session.textMessage(
                                                objectMapper.writeValueAsString(room)
                                        )
                                )));
                    } catch (JsonProcessingException e) {
                        return session.send(Mono.just(
                                session.textMessage("Error creating room: " + e.getMessage())
                                )
                        );
                    }
                });
    }

    private Mono<Void> handleJoinRoom(WebSocketSession session, long userId, Long roomId) {
        return Mono.fromCallable(() -> userRepository.findById(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optionalUser -> {
                    if (optionalUser.isEmpty()) {
                        return session.send(Mono.just(
                                session.textMessage("User not found")));
                    }

                    User user = optionalUser.get();

                    return redisTemplateForRooms.opsForValue().get(ROOM_PREFIX + String.valueOf(roomId))
                            .flatMap(room -> {
                                if (room == null) {
                                    return session.send(Mono.just(
                                            session.textMessage("Room not found: " + roomId)));
                                }

                                List<User> users = room.getUsers();
                                if (users.stream().anyMatch(u -> u.getId() == userId)) {
                                    return session.send(Mono.just(
                                            session.textMessage("User already in room")));
                                }

                                users.add(user);
                                room.setUsers(users);

                                try {
                                    return redisTemplateForRooms.opsForValue()
                                            .set(ROOM_PREFIX + String.valueOf(roomId), room)
                                            .then(redisTemplateForSessionIds.opsForValue().set(userId, session.getId()))
                                            .then(broadcastToRoom(room, user.getUsername() + " joined the room."))
                                            .and(session.send(Mono.just(
                                                    session.textMessage(
                                                            objectMapper.writeValueAsString(room)
                                                    ))));
                                } catch (JsonProcessingException e) {
                                    return session.send(Mono.just(
                                                    session.textMessage("Error join room: " + e.getMessage())
                                            )
                                    );
                                }
                            });
                });
    }

    private Mono<Void> handleQuitRoom(WebSocketSession session, long userId, String roomId) {
        return Mono.fromCallable(() -> userRepository.findById(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optionalUser -> {
                    if (optionalUser.isEmpty()) {
                        return session.send(Mono.just(
                                session.textMessage("User not found")));
                    }

                    User user = optionalUser.get();

                    return redisTemplateForRooms.opsForValue().get(roomId)
                            .flatMap(room -> {
                                if (room == null) {
                                    return session.send(Mono.just(
                                            session.textMessage("Room not found: " + roomId)));
                                }

                                List<User> users = room.getUsers();

                                boolean isInRoom = (users.stream().anyMatch(u -> u.getId() == userId));

                                if (!isInRoom) {
                                    return session.send(Mono.just(
                                            session.textMessage("User not in room")));
                                }

                                boolean isMasterPlayer = users.stream().anyMatch(u ->
                                        u.getId() == room.getMasterPlayer().getId()
                                        );

                                if (isMasterPlayer && users.size() > 1) {
                                    return session.send(Mono.just(
                                            session.textMessage("Master player can not quit room while other users remain")
                                    ));
                                }

                                users.removeIf(u -> u.getId() == userId);

                                room.setUsers(users);

                                Mono<Boolean> roomQuit;

                                if (isMasterPlayer && users.isEmpty()) {
                                    roomQuit = redisTemplateForRooms.delete(roomId).thenReturn(Boolean.TRUE);
                                } else {
                                    roomQuit = redisTemplateForRooms.opsForValue().set(roomId, room);
                                }

                                try {
                                    return roomQuit
                                            .then(redisTemplateForSessionIds.delete(userId))
                                            .then(
                                                    isMasterPlayer && users.isEmpty() ?
                                                            session.send(Mono.just(
                                                                    session.textMessage("Room is deleted and user left the room")
                                                            )) :
                                                            broadcastToRoom(room, user.getUsername() + " left the room")
                                                                    .then(session.send(Mono.just(
                                                                            session.textMessage(objectMapper.writeValueAsString(room))
                                                                    )))
                                            ).then(Mono.defer(() ->
                                                    sessionMap.remove(session.getId())
                                                            .close().then())
                                            );
                                } catch (JsonProcessingException e) {
                                    return session.send(Mono.just(
                                            session.textMessage("Error leaving room: " + e.getMessage())
                                    ));
                                }
                            });
                });
    }


}