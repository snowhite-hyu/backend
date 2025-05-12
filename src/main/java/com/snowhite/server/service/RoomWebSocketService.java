package com.snowhite.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snowhite.server.config.JwtProvider;
import com.snowhite.server.domain.Room;
import com.snowhite.server.domain.User;
import com.snowhite.server.repository.UserRepository;
import lombok.NonNull;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RoomWebSocketService implements WebSocketHandler {

    private static final AtomicLong roomIdGenerator = new AtomicLong(0);

    private final ReactiveRedisTemplate<String, Long> redisTemplateForIds;
    private final ReactiveRedisTemplate<Long, Room> redisTemplateForRooms;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    public RoomWebSocketService(
            ReactiveRedisTemplate<String, Long> redisTemplateForIds,
            ReactiveRedisTemplate<Long, Room> redisTemplateForRooms,
            UserRepository userRepository,
            JwtProvider jwtProvider
    ) {
        this.redisTemplateForIds = redisTemplateForIds;
        this.redisTemplateForRooms = redisTemplateForRooms;
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
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

        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode node = mapper.readTree(payload);
                        String action = node.get("action").asText();

                        if ("create".equals(action)) {

                            long userId = jwtProvider.extractUserIdFromToken(token);
                            int capacity = node.get("capacity").asInt();
                            int turnTime = node.get("turnTime").asInt();

                            return handleCreateRoom(session, userId, capacity, turnTime);
                        }

                        return session.send(Mono.just(
                                session.textMessage("Unsupported action: " + action)));

                    } catch (Exception e) {
                        return session.send(Mono.just(
                                session.textMessage("Invalid frame: " + e.getMessage())));
                    }
                })
                .then();
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

                    Mono<Boolean> saveRoom = redisTemplateForRooms.opsForValue().set(roomId, room);
                    Mono<Long> addRoomId = redisTemplateForIds.opsForSet().add("rooms", roomId);

                    return Mono.when(saveRoom, addRoomId)
                            .then(session.send(Mono.just(
                                    session.textMessage("Room created: " + roomId))));
                });
    }
}