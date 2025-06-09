package com.snowhite.server.websocket.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snowhite.server.security.jwt.JwtProvider;
import com.snowhite.server.service.GameService;
import com.snowhite.server.domain.enums.PlayerState;
import com.snowhite.server.payload.code.status.WsErrorStatus;
import com.snowhite.server.payload.exception.BusinessException;
import com.snowhite.server.websocket.dto.request.*;
import com.snowhite.server.websocket.dto.response.GameResponse;
import com.snowhite.server.websocket.dto.response.PlayerJoinedResponse;
import com.snowhite.server.websocket.dto.response.SecretPlayerResponse;
import com.snowhite.server.websocket.dto.response.SimpleMessageResponse;
import com.snowhite.server.payload.WsMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class GameWebSocketHandler implements WebSocketHandler {

    private static final String GAME_SESSION_PREFIX = "game-session:";

    private final GameService gameService;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    private ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplateForSessionIds;

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);

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
                case "use-rockfall-card" : {
                    RockfallCardUseRequest request = new RockfallCardUseRequest(
                            payload.get("gameId").asLong(),
                            playerId,
                            payload.get("cardId").asInt(),
                            payload.get("row").asInt(),
                            payload.get("column").asInt()
                    );
                    return handleUseRockfallCard(session, request);
                }
                case "use-map-card" : {
                    MapCardUseRequest request = new MapCardUseRequest(
                            payload.get("gameId").asLong(),
                            playerId,
                            payload.get("cardId").asInt(),
                            payload.get("row").asInt(),
                            payload.get("column").asInt()
                    );
                    return handleUseMapCard(session, request);
                }
                case "use-repair-card" : {
                    RepairCardUseRequest request = new RepairCardUseRequest(
                            payload.get("gameId").asLong(),
                            playerId,
                            payload.get("targetPlayerId").asLong(),
                            payload.get("cardId").asInt(),
                            objectMapper.treeToValue(payload.get("targetState"), PlayerState.class)
                    );
                    return handleUseRepairCard(session, request);
                }
                case "use-broken-card" : {
                    BrokenCardUseRequest request = new BrokenCardUseRequest(
                            payload.get("gameId").asLong(),
                            playerId,
                            payload.get("targetPlayerId").asLong(),
                            payload.get("cardId").asInt()
                    );
                    return handleUseBrokenCard(session, request);
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
                    return gameService.processNextRoundOrFinishGame(gameId)
                            .flatMap(nextRoundResultDTO ->  broadcastMessageToGame(gameId, "Round-Started", nextRoundResultDTO.gameResponse()));
                });
    }

    public Mono<Void> handleNextRound(WebSocketSession session, Long gameId) {
        return gameService.processNextRoundOrFinishGame(gameId)
                .flatMap(nextRoundResultDTO -> {
                    boolean isGameFinished = nextRoundResultDTO.isGameFinished();
                    // 게임이 끝난 경우
                    if (isGameFinished) {
                        return broadcastMessageToGame(gameId, "Game-Finished", nextRoundResultDTO.winnerPublicPlayerResponse());
                    }
                    // 게임을 끝나지 않고 다음 라운드 시작
                    return broadcastMessageToGame(gameId, "Round-Started", nextRoundResultDTO.gameResponse());
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

    public Mono<Void> handleUseRockfallCard(WebSocketSession session, RockfallCardUseRequest request) {
        return gameService.useRockfallCard(request)
                .flatMap(response -> {
                    Long gameId = request.gameId();
                    if (response.isRoundFinished()) {
                        return broadcastMessageToGame(gameId, "Field-Changed", response.fieldResponse())
                                .then(sendMessage(session, "Player-Info", response.secretPlayerResponse()))
                                .then(broadcastMessageToGame(gameId, "Player-Info-Changed", response.publicPlayerResponse()))
                                .then(Mono.delay(Duration.ofSeconds(5)))
                                // 역할 및 금덩이 분배 결과 공개
                                .then(broadcastMessageToGame(gameId, "Round-Finished", response.roundFinishedResponse()));
                    } else {
                        return broadcastMessageToGame(gameId, "Field-Changed", response.fieldResponse())
                                .then(sendMessage(session, "Player-Info", response.secretPlayerResponse()))
                                .then(broadcastMessageToGame(gameId, "Player-Info-Changed", response.publicPlayerResponse()))
                                .then(broadcastMessageToGame(gameId, "Turn-Changed", response.turnChangedResponse()));
                    }

                })
                .onErrorResume(e -> {
                    log.error("<rockfall card 처리 중 에러 발생>", e);
                    return sendSimpleMessage(session, "error");
                });
    }

    public Mono<Void> handleUseMapCard(WebSocketSession session, MapCardUseRequest request) {
        return gameService.useMapCard(request)
                .flatMap(response -> {
                    Long gameId = request.gameId();
                    log.info("dest card id in handler: {}", response.destCardId());
                    if (response.isRoundFinished()) {
                        return sendMessage(session, "Player-Info", response.secretPlayerResponse())
                                .then(sendMessage(session, "Dest-Card-Id", response.destCardId()))
                                .then(broadcastMessageToGame(gameId, "Player-Info-Changed", response.publicPlayerResponse()))
                                .then(Mono.delay(Duration.ofSeconds(5)))
                                // 역할 및 금덩이 분배 결과 공개
                                .then(broadcastMessageToGame(gameId, "Round-Finished", response.roundFinishedResponse()));
                    } else {
                        return sendMessage(session, "Player-Info", response.secretPlayerResponse())
                                .then(sendMessage(session, "Dest-Card-Id", response.destCardId()))
                                .then(broadcastMessageToGame(gameId, "Player-Info-Changed", response.publicPlayerResponse()))
                                .then(broadcastMessageToGame(gameId, "Turn-Changed", response.turnChangedResponse()));
                    }

                })
                .onErrorResume(e -> {
                    log.error("<map card 처리 중 에러 발생>", e);
                    return sendSimpleMessage(session, "error");
                });
    }

    public Mono<Void> handleUseRepairCard(WebSocketSession session, RepairCardUseRequest request) {
        return gameService.useRepairCard(request)
                .flatMap(response -> {
                    Long gameId = request.gameId();
                    if (response.isRoundFinished()) {
                        return sendMessage(session, "Player-Info", response.secretPlayerResponse())
                                .then(broadcastMessageToGame(gameId, "Player-Info-Changed", response.publicPlayerResponse()))
                                .then(broadcastMessageToGame(gameId, "Player-Info-Changed", response.publicTargetPlayerResponse()))
                                .then(Mono.delay(Duration.ofSeconds(5)))
                                // 역할 및 금덩이 분배 결과 공개
                                .then(broadcastMessageToGame(gameId, "Round-Finished", response.roundFinishedResponse()));
                    } else {
                        return sendMessage(session, "Player-Info", response.secretPlayerResponse())
                                .then(broadcastMessageToGame(gameId, "Player-Info-Changed", response.publicPlayerResponse()))
                                .then(broadcastMessageToGame(gameId, "Player-Info-Changed", response.publicTargetPlayerResponse()))
                                .then(broadcastMessageToGame(gameId, "Turn-Changed", response.turnChangedResponse()));
                    }

                })
                .onErrorResume(e -> {
                    log.error("<repair card 처리 중 에러 발생>", e);
                    return sendSimpleMessage(session, "error");
                });
    }

    public Mono<Void> handleUseBrokenCard(WebSocketSession session, BrokenCardUseRequest request) {
        return gameService.useBrokenCard(request)
                .flatMap(response -> {
                    Long gameId = request.gameId();
                    return sendMessage(session, "Player-Info", response.secretPlayerResponse())
                            .then(broadcastMessageToGame(gameId, "Player-Info-Changed", response.publicPlayerResponse()))
                            .then(broadcastMessageToGame(gameId, "Player-Info-Changed", response.publicTargetPlayerResponse()))
                            .then(broadcastMessageToGame(gameId, "Turn-Changed", response.turnChangedResponse()))
                            .then(Mono.delay(Duration.ofSeconds(5)))
                            // 역할 및 금덩이 분배 결과 공개
                            .then(broadcastMessageToGame(gameId, "Round-Finished", response.roundFinishedResponse()));
                })
                .onErrorResume(e -> {
                    log.error("<repair card 처리 중 에러 발생>", e);
                    return sendSimpleMessage(session, "error");
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
                                .then(sendMessage(session, "Player-Info", usePathCardResultDTO.secretPlayerResponse()))
                                .then(broadcastMessageToGame(gameId, "Player-Info-Changed", usePathCardResultDTO.publicPlayerResponse()))
                                .then(Mono.delay(Duration.ofSeconds(5)))
                                // 역할 및 금덩이 분배 결과 공개
                                .then(broadcastMessageToGame(gameId, "Round-Finished", usePathCardResultDTO.roundFinishedResponse()));
                    }

                    // 카드를 놓은 후 다음 턴 진행
                    return broadcastMessageToGame(gameId, "Field-Changed", usePathCardResultDTO.fieldResponse())
                            .then(sendMessage(session, "Player-Info", usePathCardResultDTO.secretPlayerResponse()))
                            .then(broadcastMessageToGame(gameId, "Player-Info-Changed", usePathCardResultDTO.publicPlayerResponse()))
                            .then(broadcastMessageToGame(gameId, "Turn-Changed", usePathCardResultDTO.turnChangedResponse()));
                });
    }

    // 카드 가져오기
    public Mono<Void> handleGetCard(WebSocketSession session, Long gameId, Long playerId) {
        return gameService.getCard(gameId, playerId)
                .flatMap(player -> sendMessage(session, "Got-Card", player));
    }

    public Mono<Void> handleDropCard(WebSocketSession session, Long gameId, Long playerId, Integer cardId) {
        return gameService.dropCard(gameId, playerId, cardId)
                .flatMap(dropCardResultDTO -> {

                    // 카드를 버린 후 라운드가 끝나는 경우
                    if (dropCardResultDTO.isRoundFinished()) {
                        return sendMessage(session, "Player-Info", dropCardResultDTO.secretPlayerResponse())
                                .then(broadcastMessageToGame(gameId, "Player-Info-Changed", dropCardResultDTO.publicPlayerResponse()))
                                .then(Mono.delay(Duration.ofSeconds(5)))
                                .then(broadcastMessageToGame(gameId, "Round-Finished", dropCardResultDTO.roundFinishedResponse()));
                    }

                    // 카드를 버린 후 다음 턴 진행
                    return (sendMessage(session, "Player-Info", dropCardResultDTO.secretPlayerResponse()))
                            .then(broadcastMessageToGame(gameId, "Player-Info-Changed", dropCardResultDTO.publicPlayerResponse()))
                            .then(broadcastMessageToGame(gameId, "Turn-Changed", dropCardResultDTO.turnChangedResponse()));
                });
    }
    // 게임 전체에 broadcast
    public Mono<Void> broadcastMessageToGame(Long gameId, String type, Object payload) {
        if (payload == null) {
            return Mono.error(new BusinessException(WsErrorStatus.INTERNAL_ERROR));
        }

        return gameService.getGameByGameId(gameId)
                .flatMapMany(game -> Flux.fromIterable(game.getPlayers()))
                .flatMap(player -> {
                    Long playerId = player.getPlayerId();
                    return reactiveRedisTemplateForSessionIds.opsForValue().get(GAME_SESSION_PREFIX + playerId)
                            .flatMap(sessionId -> {
                                if(sessionId == null) return Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST));
                                WebSocketSession sessionToSend = sessionMap.get(sessionId);
                                if(sessionToSend == null) return Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST));
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
