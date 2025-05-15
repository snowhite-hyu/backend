package com.snowhite.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snowhite.server.config.JwtProvider;
import com.snowhite.server.domain.Room;
import com.snowhite.server.domain.User;
import com.snowhite.server.domain.UserRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Slf4j
@Component
public class RoomWebSocketService implements WebSocketHandler {

    private static final AtomicLong roomIdGenerator = new AtomicLong(0);

    private final ReactiveRedisTemplate<String, Room> redisTemplateForRooms;
    private final ReactiveRedisTemplate<Long, String> redisTemplateForSessionIds;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    public RoomWebSocketService(
            @Qualifier("reactiveRedisTemplateForRooms")
            ReactiveRedisTemplate<String, Room> redisTemplateForRooms,
            @Qualifier("reactiveRedisTemplateForSessionIds")
            ReactiveRedisTemplate<Long, String> redisTemplateForSessionIds,
            UserRepository userRepository,
            JwtProvider jwtProvider
    ) {
        this.redisTemplateForRooms = redisTemplateForRooms;
        this.redisTemplateForSessionIds = redisTemplateForSessionIds;
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
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(payload);
                String action = node.get("action").asText().toLowerCase();

                switch (action) {
                    case "create":
                    {
                        long userId = jwtProvider.extractUserIdFromToken(token);
                        int capacity = node.get("capacity").asInt();
                        int turnTime = node.get("turnTime").asInt();

                        return handleCreateRoom(session, userId, capacity, turnTime);
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
                    String roomId = "room:" + String.valueOf(roomIdGenerator.incrementAndGet());
                    List<User> users = new ArrayList<>();
                    users.add(user);

                    Room room = new Room(roomId, user, users, capacity, turnTime, false);

                    Mono<Boolean> saveRoom = redisTemplateForRooms.opsForValue().set(roomId, room);

                    return Mono.when(saveRoom)
                            .then(session.send(Mono.just(
                                    session.textMessage("Room created: " + roomId))));
                });
    }
}