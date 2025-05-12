package com.snowhite.server.service;

import com.snowhite.server.config.JwtProvider;
import com.snowhite.server.domain.Room;
import com.snowhite.server.domain.User;
import com.snowhite.server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.reactive.socket.*;
import reactor.core.publisher.*;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.mockito.Mockito.*;

import org.mockito.Mockito;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.WebSocketMessage;

import static org.mockito.ArgumentMatchers.any;

public class RoomWebSocketServiceTest {

    private RoomWebSocketService roomWebSocketService;
    private ReactiveRedisTemplate<String, Long> redisTemplateForIds;
    private ReactiveRedisTemplate<Long, Room> redisTemplateForRooms;
    private UserRepository userRepository;
    private JwtProvider jwtProvider;
    private WebSocketSession session;
    private HandshakeInfo handshakeInfo;

    @BeforeEach
    void setUp() {
        redisTemplateForIds = Mockito.mock(ReactiveRedisTemplate.class);
        redisTemplateForRooms = Mockito.mock(ReactiveRedisTemplate.class);
        userRepository = Mockito.mock(UserRepository.class);
        jwtProvider = Mockito.mock(JwtProvider.class);
        session = Mockito.mock(WebSocketSession.class);
        handshakeInfo = Mockito.mock(HandshakeInfo.class);

        roomWebSocketService = new RoomWebSocketService(
                redisTemplateForIds,
                redisTemplateForRooms,
                userRepository,
                jwtProvider
        );
    }

    @Test
    void testHandleCreateRoomSuccess() {
        String token = "validToken";
        String uri = "ws://localhost/rooms?token=" + token;

        when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
        when(handshakeInfo.getUri()).thenReturn(URI.create(uri));
        when(jwtProvider.isTokenValid(token)).thenReturn(true);
        when(jwtProvider.extractUserIdFromToken(token)).thenReturn(1L);

        User user = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(redisTemplateForRooms.opsForValue()).thenReturn(Mockito.mock(ReactiveValueOperations.class));
        when(redisTemplateForIds.opsForSet()).thenReturn(Mockito.mock(ReactiveSetOperations.class));

        when(redisTemplateForRooms.opsForValue().set(any(Long.class), any(Room.class))).thenReturn(Mono.just(true));
        when(redisTemplateForIds.opsForSet().add(any(String.class), any(Long.class))).thenReturn(Mono.just(1L));

        String createRoomPayload = "{\"action\":\"create\", \"capacity\":5, \"turnTime\":30}";
        WebSocketMessage message = Mockito.mock(WebSocketMessage.class);
        when(message.getPayloadAsText()).thenReturn(createRoomPayload);

        when(session.receive()).thenReturn(Flux.just(message));
        when(session.textMessage(any())).thenReturn(message);
        when(session.send(any())).thenReturn(Mono.empty());

        Mono<Void> result = roomWebSocketService.handle(session);

        StepVerifier.create(result)
                .verifyComplete();

        verify(session).send(any());
    }
}
