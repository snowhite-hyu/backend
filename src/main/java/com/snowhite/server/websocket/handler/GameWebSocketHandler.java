package com.snowhite.server.websocket.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snowhite.server.domain.session.Game;
import com.snowhite.server.repository.CardRepository;
import com.snowhite.server.security.jwt.JwtProvider;
import com.snowhite.server.service.GameService;
import com.snowhite.server.websocket.dto.response.GameResponse;
import com.snowhite.server.domain.enums.PlayerState;
import com.snowhite.server.websocket.dto.request.ActionCardUseRequest;
import com.snowhite.server.websocket.dto.response.ActionCardUsedResponse;
import com.snowhite.server.websocket.dto.response.PlayerJoinedResponse;
import com.snowhite.server.websocket.dto.response.SecretPlayerResponse;
import com.snowhite.server.websocket.dto.response.SimpleMessageResponse;
import com.snowhite.server.payload.WsMessage;
import com.snowhite.server.websocket.dto.response.nextround.NextRoundGameResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class GameWebSocketHandler implements WebSocketHandler {

    private static final String GAME_PREFIX = "game:";

    private final CardRepository cardRepository;
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
                .then(session.receive()
                        .doFinally(signalType -> {
                            sessionMap.remove(session.getId());
                            reactiveRedisTemplateForSession.delete(userId).subscribe();
                        })
                        .map(WebSocketMessage::getPayloadAsText)
                        .flatMap(message -> handleMessage(session, message))
                        .then()
                );
    }

    // type에 따라 요청 처리
    public Mono<Void> handleMessage(WebSocketSession session, String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String type = root.get("type").asText();
            JsonNode payload = root.get("payload");

            switch (type) {
                case "join-game": {
                    long gameId = payload.get("gameId").asLong();
                    long playerId = payload.get("playerId").asLong();
                    return handleJoinGame(session, gameId, playerId);
                }

                case "start-round": {
                    long gameId = payload.get("gameId").asLong();
                    return handleNextRound(session, gameId);
                }

                case "get-game-state": {
                    long gameId = payload.get("gameId").asLong();
                    return handleGetGameState(session, gameId);
                }

                case "get-player-info": {
                    long gameId = payload.get("gameId").asLong();
                    long playerId = payload.get("playerId").asLong();
                    return handleGetPlayerInfo(session, gameId, playerId);
                }
                case "use-action-card" : {
                    long gameId = Long.parseLong(payload.get("gameId").asText());
                    ActionCardUseRequest request = new ActionCardUseRequest(
                            payload.get("cardId").asInt(),
                            payload.get("usePlayerId").asLong(),
                            payload.has("targetPlayerId") ? payload.get("targetPlayerId").asLong() : null,
                            payload.has("locationX") ? payload.get("locationX").asInt() : null,
                            payload.has("locationY") ? payload.get("locationY").asInt() : null,
                            payload.has("targetRepairState") ?
                                    objectMapper.treeToValue(payload.get("targetRepairState"), PlayerState.class) : null
                    );
                    return handleUseActionCard(session, gameId, request);
                }

                case "get-card": {
                    long gameId = Long.parseLong(payload.get("gameId").asText());
                    long playerId = Long.parseLong(payload.get("playerId").asText());
                    return handleGetCard(session, gameId, playerId);
                }

                case "drop-card": {
                    long gameId = Long.parseLong(payload.get("gameId").asText());
                    long playerId = Long.parseLong(payload.get("playerId").asText());
                    int cardId = Integer.parseInt(payload.get("cardId").asText());
                    return handleDropCard(session, gameId, playerId, cardId);
                }

                default:
                    return sendMessage(session, "error", null);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Mono.error(e);   // TODO: 예외 처리
        }
    }

    // Player를 Game에 Join 후 남은 Player 전송
    public Mono<Void> handleJoinGame(WebSocketSession session, Long gameId, Long playerId) {

        return gameService.joinPlayer(gameId, playerId)
                .flatMap(playersLeft -> {
                    if (playersLeft > 0) {
                        PlayerJoinedResponse result = PlayerJoinedResponse.of(playersLeft);
                        return broadcastMessageToGame(gameId, "Game-Joined", result);
                    }
                    return gameService.processNextRoundOrFinishRound(gameId)
                            .flatMap(result -> broadcastMessageToGame(gameId, "Round-Started", result));
                });
    }

    public Mono<Void> handleNextRound(WebSocketSession session, Long gameId) {

        return gameService.processNextRoundOrFinishRound(gameId)
                .flatMap(result -> {
                    if (result instanceof NextRoundGameResponse) {
                        return broadcastMessageToGame(gameId, "Round-Started", result);
                    } else {    // result instanceof NextRoundPlayersResponse
                        return broadcastMessageToGame(gameId, "Round-Finished", result);
                    }
                });

    }

    public Mono<Void> handleGetGameState(WebSocketSession session, Long gameId) {

        return gameService.getGameByGameId(gameId)
                .flatMap(game -> {
                    GameResponse result = GameResponse.from(game);
                    return sendMessage(session, "Game-State", result);
                });
    }

    public Mono<Void> handleGetPlayerInfo(WebSocketSession session, Long gameId, Long playerId) {

        return gameService.findPlayerByGameIdAndPlayerId(gameId, playerId)
                .flatMap(player -> {
                    SecretPlayerResponse result = SecretPlayerResponse.from(player);
                    return sendMessage(session, "Player-Info", result);
                });
    }

    public Mono<Void> handleUseActionCard(WebSocketSession session, Long gameId, ActionCardUseRequest request) {
        return gameService.useActionCard(gameId, request)
                .flatMap(response -> {
                    ActionCardUsedResponse unicastResponse = ActionCardUsedResponse.ofUnicast(
                            response.gameId(),
                            response.message(),
                            response.actionCardId(),
                            response.usePlayerId(),
                            response.usePlayerCards()
                    );
                    ActionCardUsedResponse broadcastResponse = ActionCardUsedResponse.ofBroadcast(
                            response.gameId(),
                            response.message(),
                            response.actionCardId(),
                            response.targetPlayerId(),
                            response.targetPlayerState(),
                            response.field()
                    );
                    Mono<Void> uni = sendMessage(session, "[Unicast]: Action-Card-Use", unicastResponse);
                    Mono<Void> broad = broadcastMessageToGame(gameId, "[Broadcast]: Action-Card-Use", broadcastResponse);
                    return Mono.when(uni, broad);
                });
    }

    // 카드 가져오기
    public Mono<Void> handleGetCard(WebSocketSession session, Long gameId, Long playerId) {
        return gameService.getCard(gameId, playerId)
                .flatMap(player -> sendMessage(session, "Got-Card", player));
    }

    public Mono<Void> handleDropCard(WebSocketSession session, Long gameId, Long playerId, Integer cardId) {
        return gameService.dropCard(gameId, playerId, cardId)
                .flatMap(player -> sendMessage(session, "Card-Dropped", player));
    }
    // 게임 전체에 broadcast
    public Mono<Void> broadcastMessageToGame(Long gameId, String type, Object payload) {

        return gameService.getGameByGameId(gameId)
                .flatMapMany(game -> Flux.fromIterable(game.getPlayers()))
                .flatMap(player -> {
                    Long playerId = player.getPlayerId();
                    return reactiveRedisTemplateForSession.opsForValue().get(playerId)
                            .flatMap(sessionId -> {
                                WebSocketSession sessionToSend = sessionMap.get(sessionId);
                                return sendMessage(sessionToSend, type, payload);
                            });
                })
                .then();
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

    // type, payload 동시에 직렬화 후 메시지 전송
    public Mono<Void> sendMessage(WebSocketSession session, String type, Object payload) {

        WsMessage<Object> result = WsMessage.onSuccess(type, payload);
        try {
            String json = objectMapper.writeValueAsString(result);
            return session.send(Mono.just(
                    session.textMessage(json)
            ));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Mono.error(e);
        }
    }
}
