package com.snowhite.server.web.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snowhite.server.domain.session.Game;
import com.snowhite.server.security.jwt.JwtProvider;
import com.snowhite.server.service.GameService;
import com.snowhite.server.web.dto.websocket.response.SimpleMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class GameWebSocketHandler implements WebSocketHandler {

    private final GameService gameService;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    private final ReactiveRedisTemplate<String, Game> reactiveRedisTemplateForGame;
    private final ReactiveRedisTemplate<Long, String> reactiveRedisTemplateForSession;

    // 세션 저장 후 처리
    @Override
    public Mono<Void> handle(WebSocketSession session) {

        String uri = session.getHandshakeInfo().getUri().toString();
        String token = jwtProvider.extractTokenFromURI(uri);
        Long userId = jwtProvider.extractUserIdFromToken(token);

        return reactiveRedisTemplateForSession.opsForValue().set(userId, session.getId())
                .doOnSuccess(ignored -> sessionMap.put(session.getId(), session))
                .then(
                        session.receive()
                                .doFinally(signalType -> {
                                    sessionMap.remove(session.getId());
                                    reactiveRedisTemplateForSession.delete(userId).subscribe();
                                })
                                .map(WebSocketMessage::getPayloadAsText)
                                .flatMap(payload -> handlePayload(session, payload))
                                .then()
                );
    }

    // type에 따라 요청 처리
    public Mono<Void> handlePayload(WebSocketSession session, String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.get("type").asText();

            switch (type) {
                case "join-game":
                    long gameId = Long.parseLong(node.get("gameId").asText());
                    long playerId = Long.parseLong(node.get("playerId").asText());
                    return handleJoinGame(session, gameId, playerId);

                default:
                    return sendSimpleMessage(session, "undefined type");
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Mono.error(e);   // TODO: 예외 처리
        }
    }

    // 단순 문자열 전송
    public Mono<Void> sendSimpleMessage(WebSocketSession session, String message) {

        SimpleMessageResponse simpleMessageResponse = SimpleMessageResponse.of(message);
        try {
            String payload = objectMapper.writeValueAsString(simpleMessageResponse);
            return session.send(Mono.just(
                    session.textMessage(payload)
            ));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Mono.error(e);   // TODO: 예외 처리
        }
    }

    // 클래스(DTO 등)를 JSON으로 변환 후 전송
    public Mono<Void> sendObjectMessage(WebSocketSession session, Object object) {

        try {
            String payload = objectMapper.writeValueAsString(object);
            return session.send(Mono.just(
                    session.textMessage(payload)
            ));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Mono.error(e);   // TODO: 예외 처리
        }
    }

    // Player를 Game에 Join 후 남은 Player 전송
    public Mono<Void> handleJoinGame(WebSocketSession session, Long gameId, Long playerId) {

        return gameService.joinPlayer(gameId, playerId)
                .flatMap(playersLeft -> {
                    if (playersLeft == 0) {
                        return sendSimpleMessage(session, "All Joined");
                    } else {
                        return sendSimpleMessage(session, playersLeft + " Players Left");
                    }
                });

    }

}
