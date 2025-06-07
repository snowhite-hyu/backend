package com.snowhite.server.websocket.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snowhite.server.security.jwt.JwtProvider;
import com.snowhite.server.service.GameService;
import com.snowhite.server.websocket.dto.response.*;
import com.snowhite.server.domain.enums.PlayerState;
import com.snowhite.server.websocket.dto.request.ActionCardUseRequest;
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

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class GameWebSocketHandler implements WebSocketHandler {

    private static final String GAME_SESSION_PREFIX = "game-session:";

    private final GameService gameService;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplateForSessionIds;


    // 세션 저장 후 처리
    @Override
    public Mono<Void> handle(WebSocketSession session) {

        String uri = session.getHandshakeInfo().getUri().toString();
        String token = jwtProvider.extractTokenFromURI(uri);
        Long userId = jwtProvider.extractUserIdFromToken(token);

        return reactiveRedisTemplateForSessionIds.opsForValue().set(GAME_SESSION_PREFIX + userId, session.getId())
                .doOnSuccess(ignored -> sessionMap.put(session.getId(), session))
                .then(session.receive()
                        .doFinally(signalType -> {
                            sessionMap.remove(session.getId());
                            reactiveRedisTemplateForSessionIds.delete(GAME_SESSION_PREFIX + userId).subscribe();
                        })
                        .map(WebSocketMessage::getPayloadAsText)
                        .flatMap(message -> handleMessage(session, message, userId))
                        .then()
                );
    }

    // type에 따라 요청 처리
    public Mono<Void> handleMessage(WebSocketSession session, String message, Long playerId) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String type = root.get("type").asText();
            JsonNode payload = root.get("payload");

            switch (type) {
                case "join-game": {
                    long gameId = payload.get("gameId").asLong();
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

                case "use-path-card": {
                    long gameId = payload.get("gameId").asLong();
                    int cardId = payload.get("cardId").asInt();
                    int row = payload.get("row").asInt();
                    int column = payload.get("column").asInt();
                    int isFlipped = payload.get("isFlipped").asInt();

                    return handleUsePathCard(session, gameId, playerId, cardId, row, column, isFlipped);
                }

                case "get-card": {
                    long gameId = Long.parseLong(payload.get("gameId").asText());
                    return handleGetCard(session, gameId, playerId);
                }

                case "drop-card": {
                    long gameId = Long.parseLong(payload.get("gameId").asText());
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

    public Mono<Void> handleUsePathCard(WebSocketSession session, Long gameId, Long playerId, Integer cardId, Integer row, Integer column, Integer isFlipped) {

        return gameService.processUsePathCard(gameId, playerId, cardId, row, column, isFlipped)
                .flatMap(usePathCardResultDTO -> {
                    // 요청된 위치에 카드를 놓지 못하는 경우
                    if (!usePathCardResultDTO.isPossibleToPlace()) {
                        return sendMessage(session, "Place-PathCard-Failed", usePathCardResultDTO.fieldResponse());
                    }

                    // 카드를 놓은 후 라운드가 끝나는 경우
                    if (usePathCardResultDTO.isRoundFinished()) {
                        return broadcastMessageToGame(gameId, "Field-Changed", usePathCardResultDTO.fieldResponse())
                                .then(broadcastMessageToGame(gameId, "Player-Info-Changed", usePathCardResultDTO.publicPlayerResponse()))
                                .then(Mono.delay(Duration.ofSeconds(5)))
                                // 역할 및 금덩이 분배 결과 공개
                                .then(broadcastMessageToGame(gameId, "Round-Finished", usePathCardResultDTO.roundFinishedResponse()));
                    }
                    return broadcastMessageToGame(gameId, "Field-Changed", usePathCardResultDTO.fieldResponse())
                            .then(broadcastMessageToGame(gameId, "Player-Info-Changed", usePathCardResultDTO.publicPlayerResponse()));
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
                    return reactiveRedisTemplateForSessionIds.opsForValue().get(GAME_SESSION_PREFIX + playerId)
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
