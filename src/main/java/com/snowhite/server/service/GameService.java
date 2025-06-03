package com.snowhite.server.service;

import com.snowhite.server.domain.entity.ActionCard;
import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.enums.PlayerState;
import com.snowhite.server.domain.enums.ActionCardType;
import com.snowhite.server.domain.session.Game;
import com.snowhite.server.domain.session.Player;
import com.snowhite.server.websocket.dto.response.GameResponse;
import com.snowhite.server.websocket.dto.response.SecretPlayerResponse;
import com.snowhite.server.websocket.dto.response.nextround.NextRoundGameResponse;
import com.snowhite.server.websocket.dto.response.nextround.NextRoundPlayersResponse;
import com.snowhite.server.websocket.dto.response.nextround.NextRoundResponse;
import com.snowhite.server.payload.code.status.WsErrorStatus;
import com.snowhite.server.payload.exception.BusinessException;
import com.snowhite.server.payload.exception.WebSocketException;
import com.snowhite.server.websocket.dto.request.*;
import com.snowhite.server.websocket.dto.response.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final String GAME_PREFIX = "game:";
    private static final String CARD_PREFIX = "card:";

    private final ReactiveRedisTemplate<String, Game> reactiveRedisTemplateForGame;
    private final ReactiveRedisTemplate<String, Card> reactiveRedisTemplateForCard;

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    // 카드 가져오기
    public Mono<Player> getCard(Long gameId, Long playerId) {
        String gameKey = GAME_PREFIX + gameId;
        return reactiveRedisTemplateForGame.opsForValue().get(gameKey)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("게임이 없음")))
                .flatMap(game -> {
                    Optional<Player> optionalPlayer = game.findPlayer(playerId);
                    if (optionalPlayer.isEmpty()) {
                        return Mono.error(new IllegalArgumentException("플레이어가 없음"));
                    }
                    Player player = optionalPlayer.get();
                    Optional<Integer> optionalCard = game.drawCard();
                    if (optionalCard.isEmpty()) {
                        return Mono.error(new IllegalArgumentException("남아있는 카드가 없음"));
                    }
                    player.addCardToHand(optionalCard.get());
                    game.nextTurn();
                    return reactiveRedisTemplateForGame.opsForValue().set(gameKey, game).thenReturn(player);
                });
    }

    // 카드 버리기
    public Mono<Player> dropCard(Long gameId, Long playerId, int cardId) {
        String gameKey = GAME_PREFIX + gameId;
        return reactiveRedisTemplateForGame.opsForValue().get(gameKey)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("게임이 없음")))
                .flatMap(game -> {
                    Optional<Player> optionalPlayer = game.findPlayer(playerId);
                    if (optionalPlayer.isEmpty()) {
                        return Mono.error(new IllegalArgumentException("플레이어가 없음"));
                    }
                    Player player = optionalPlayer.get();
                    if (!player.dropCard(cardId)) {
                        return Mono.error(new IllegalArgumentException("해당 카드가 없음"));
                    }
                    game.nextTurn();
                    return reactiveRedisTemplateForGame.opsForValue().set(gameKey, game).thenReturn(player);
                });
    }

    // game에 player를 join시킨 후 남은 player 수 리턴
    public Mono<Integer> joinPlayer(Long gameId, Long playerId) {

        return getGameByGameId(gameId)
                .map(game -> {
                    int remain = game.joinPlayerAndReturnRemain(playerId);
                    setGameToRedis(gameId, game);
                    return remain;
                });
    }

    // 해당 game에 모든 player가 join했는지 확인
    public Mono<Boolean> verifyAllJoined(Long gameId) {
        return getGameByGameId(gameId)
                .map(game -> game.getJoinedPlayerIds().size() == game.getPlayers().size());
    }

    // 새로운 round 시작 또는 round 종료
    public Mono<NextRoundResponse> processNextRoundOrFinishRound(Long gameId) {

        return getGameByGameId(gameId)
                .flatMap(game -> {
                    boolean isFinished = game.startNextRoundAndReturnGameFinished();
                    if (isFinished) {
                        return setGameToRedis(gameId, game)
                                .then(getAllSecretPlayerInfo(game))
                                .map(NextRoundPlayersResponse::of);
                    } else {
                        return setGameToRedis(gameId, game)
                                .thenReturn(NextRoundGameResponse.of(GameResponse.from(game)));
                    }
                });
    }

    public Mono<List<SecretPlayerResponse>> getAllSecretPlayerInfo(Game game) {

        return Mono.just(
                game.getPlayers().stream()
                        .map(SecretPlayerResponse::from)
                        .toList()
        );
    }

    public Mono<Game> getGameByGameId(Long gameId) {
        return reactiveRedisTemplateForGame.opsForValue().get(GAME_PREFIX + gameId);
    }

    public Mono<Boolean> setGameToRedis(Long gameId, Game game) {
        return reactiveRedisTemplateForGame.opsForValue().set(GAME_PREFIX + gameId, game);
    }

    public Mono<Player> findPlayerByGameIdAndPlayerId(Long gameId, Long playerId) {
        return getGameByGameId(gameId)
                .map(game -> game.findPlayer(playerId).get());
    }


    // 게임에 필요한 카드 정보 가져오기
    private Flux<Card> getAllCardsFromRedis() {
        return reactiveRedisTemplateForCard
                .scan(ScanOptions.scanOptions().match(CARD_PREFIX).build())
                .flatMap(key -> reactiveRedisTemplateForCard.opsForValue().get(key));
    }

    private Player findPlayerByPlayerId(Game game, Long playerId) {
        return game.getPlayers().stream()
                .filter(player -> player.getPlayerId() == playerId)
                .findFirst()
                .orElse(null);
    }

    private Mono<Card> findCardByCardId(int cardId) {
        String key = CARD_PREFIX + cardId;
        return reactiveRedisTemplateForCard.opsForValue()
                .get(key)
                .switchIfEmpty(Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST)));
    }

    private Mono<Boolean> saveGameToRedis(Game game) {
        return reactiveRedisTemplateForGame.opsForValue()
                .set(GAME_PREFIX + game.getGameId(), game);
    }

    private List<PlayerState> getRepairStates(ActionCardType type) {
        return switch (type) {
            case REPAIR_PICKAXE -> List.of(PlayerState.BROKEN_PICKAXE);
            case REPAIR_LANTERN -> List.of(PlayerState.BROKEN_LANTERN);
            case REPAIR_MINECART -> List.of(PlayerState.BROKEN_MINECART);
            case REPAIR_PICKAXE_AND_LANTERN -> List.of(PlayerState.BROKEN_PICKAXE, PlayerState.BROKEN_LANTERN);
            case REPAIR_PICKAXE_AND_MINECART -> List.of(PlayerState.BROKEN_PICKAXE, PlayerState.BROKEN_MINECART);
            case REPAIR_LANTERN_MINECART -> List.of(PlayerState.BROKEN_LANTERN, PlayerState.BROKEN_MINECART);
            default -> List.of();
        };
    }

    private List<PlayerState> getBrokenStates(ActionCardType type) {
        return switch (type) {
            case BROKEN_PICKAXE -> List.of(PlayerState.BROKEN_PICKAXE);
            case BROKEN_LANTERN -> List.of(PlayerState.BROKEN_LANTERN);
            case BROKEN_MINECART -> List.of(PlayerState.BROKEN_MINECART);
            default -> List.of();
        };
    }

    public Mono<RockfallCardUsedResponse> useRockfallCard(RockfallCardUseRequest request) {
        try {
            Long gameId = request.gameId();
            Long playerId = request.playerId();
            int cardId = request.cardId();
            Integer row = request.row();
            Integer column = request.column();

            log.info("[Rockfall] 카드 사용 요청 - gameId: {}, playerId: {}, cardId: {}, row: {}, column: {}",
                    gameId, playerId, cardId, row, column);

            return getGameByGameId(gameId)
                    .flatMap(game -> {
                        Player player = findPlayerByPlayerId(game, playerId);
                        log.info("[Rockfall] 플레이어 조회 성공 - playerId: {}", playerId);
                        if (game.getPathCardIdAt(row, column) == -1) {
                            log.warn("[Rockfall] 해당 위치에 카드 없거나 사용 불가한 위치 - row: {}, column: {}", row, column);
                            return Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST));
                        }

                        game.removeCard(row, column);
                        player.removeCard(cardId);
                        log.info("[Rockfall] 카드 제거 완료 - cardId: {}, 위치: ({}, {})", cardId, row, column);

                        return saveGameToRedis(game)
                                .flatMap(success -> {
                                    if (success) {
                                        RockfallCardUsedResponse response = new RockfallCardUsedResponse(
                                                gameId, playerId, cardId, row, column, player.getHand(), game.getField()
                                        );
                                        log.info("[Rockfall] 카드 사용 완료 - 응답 생성");
                                        return Mono.just(response);
                                    } else {
                                        log.error("[Rockfall] 게임 Redis 저장 실패");
                                        return Mono.error(new BusinessException(WsErrorStatus.INTERNAL_ERROR));
                                    }
                                });
                    })
                    .onErrorResume(e -> {
                        log.error("[Rockfall] 예외 발생: {}", e.getMessage(), e);
                        return Mono.error(e);
                    });

        } catch (BusinessException e) {
            log.warn("[Rockfall] 비즈니스 예외 발생: {}", e.getMessage(), e);
            return Mono.error(e);
        } catch (Exception e) {
            log.error("[Rockfall] 알 수 없는 예외 발생", e);
            return Mono.error(new BusinessException(WsErrorStatus.INTERNAL_ERROR));
        }
    }

    public Mono<MapCardUsedResponse> useMapCard(MapCardUseRequest request) {
        try {
            Long gameId = request.gameId();
            Long playerId = request.playerId();
            int cardId = request.cardId();
            Integer row = request.row();
            Integer column = request.column();
            log.info("[Map] 카드 사용 요청 - gameId: {}, playerId: {}, cardId: {}, row: {}, column: {}",
                    gameId, playerId, cardId, row, column);

            return getGameByGameId(gameId)
                    .flatMap(game -> {
                        try {
                            Player player = findPlayerByPlayerId(game, playerId);
                            log.info("[Map] 플레이어 조회 성공 - playerId: {}", playerId);
                            
                            if (game.getDestCardIdAt(row, column) == -1) {
                                log.warn("[Map] 해당 위치에 카드가 없거나 사용 불가한 위치 - row: {}, column: {}", row, column);
                                return Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST));
                            }
                            if (game.isFlipped(row, column)) {
                                log.warn("[Map] 해당 위치에 카드가 이미 공개됨 - row: {}, column: {}", row, column);
                                return Mono.error(new BusinessException(WsErrorStatus.CANNOT_USE_CARD));
                            }
                            player.removeCard(cardId);
                            log.info("[Map] player로부터 카드 제거 완료 - cardId: {})", cardId);

                            return saveGameToRedis(game)
                                    .flatMap(success -> {
                                        if (success) {
                                            MapCardUsedResponse response = new MapCardUsedResponse(
                                                    gameId, playerId, cardId, row, column, player.getHand(), game.getDestCardIdAt(row, column)
                                            );
                                            log.info("[Map] 카드 사용 완료 - 응답 생성");
                                            return Mono.just(response);
                                        } else {
                                            return Mono.error(new BusinessException(WsErrorStatus.INTERNAL_ERROR));
                                        }
                                    })
                                    .onErrorResume(e -> {
                                        // saveGameToRedis 에서 발생한 예외
                                        if (e instanceof BusinessException) return Mono.error(e);
                                        log.error("[Map] 게임 Redis 저장 실패");
                                        return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
                                    });

                        } catch (BusinessException e) {
                            return Mono.error(e);
                        } catch (Exception e) {
                            log.error("[Map] 예외 발생: {}", e.getMessage(), e);
                            return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
                        }
                    })
                    .onErrorResume(e -> {
                        // getGameByGameId 또는 전체 체인 예외 처리
                        if (e instanceof BusinessException) return Mono.error(e);
                        log.warn("[Map] 비즈니스 예외 발생: {}", e.getMessage(), e);
                        return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
                    });
        } catch (Exception e) {
            log.error("[Map] 알 수 없는 예외 발생", e);
            return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
        }
    }

    public Mono<RepairCardUsedResponse> useRepairCard(RepairCardUseRequest request) {
        Long gameId = request.gameId();
        Long playerId = request.playerId();
        int cardId = request.cardId();
        Long targetPlayerId = request.targetPlayerId();
        PlayerState targetState = request.targetState();

        log.info("[Repair] 카드 사용 요청 - gameId: {}, playerId: {}, targetPlayerId: {}, cardId: {}, targetState: {}",
                gameId, playerId, targetPlayerId, cardId, targetState);

        return getGameByGameId(gameId)
                .flatMap(game -> {
                    try {
                        Player player = findPlayerByPlayerId(game, playerId);
                        Player targetPlayer = findPlayerByPlayerId(game, targetPlayerId);
                        log.info("[Repair] 플레이어 조회 성공 - playerId: {}, targetPlayerId: {}", playerId, targetPlayerId);

                        return findCardByCardId(cardId)
                                .switchIfEmpty(Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST)))
                                .flatMap(card -> {
                                    log.info("[Repair] 카드 조회 성공 - cardId: {}", cardId);
                                    try {

                                        ActionCard repairCard = (ActionCard) card;

                                        List<PlayerState> repairStates = getRepairStates(repairCard.getActionCardType());
                                        log.info("[Repair] 카드 타입 검사 - repairStates: {}", repairStates);

                                        boolean hasRepairableState = targetPlayer.getState().stream()
                                                .anyMatch(repairStates::contains);

                                        if (!hasRepairableState || !repairStates.contains(targetState)) {
                                            log.warn("[Repair] 대상 상태가 수리 대상이 아님 - targetState: {}, 현재 상태: {}",
                                                    targetState, targetPlayer.getState());
                                            return Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST));
                                        }

                                        targetPlayer.removePlayerState(targetState);
                                        player.removeCard(cardId);
                                        log.info("[Repair] 상태 복구 및 카드 제거 완료 - playerId: {}, targetPlayerId: {}, cardId: {}",
                                                playerId, targetPlayerId, cardId);
                                        log.info("[Repair] targetPlayerState 전체 출력: {}", targetPlayer.getState());

                                        return saveGameToRedis(game)
                                                .flatMap(success -> {
                                                    if (success) {
                                                        log.info("[Repair] Redis 저장 완료");
                                                        return Mono.just(new RepairCardUsedResponse(
                                                                gameId, playerId, targetPlayerId, cardId, player.getHand(), targetPlayer.getState()
                                                        ));
                                                    } else {
                                                        log.error("[Repair] Redis 저장 실패");
                                                        return Mono.error(new BusinessException(WsErrorStatus.INTERNAL_ERROR));
                                                    }
                                                })
                                                .onErrorResume(e -> {
                                                    if (e instanceof BusinessException) return Mono.error(e);
                                                    log.error("[Repair] Redis 저장 중 예외 발생", e);
                                                    return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
                                                });

                                    } catch (Exception e) {
                                        log.error("[Repair] 카드 처리 중 예외 발생", e);
                                        return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
                                    }
                                })
                                .onErrorResume(e -> {
                                    if (e instanceof BusinessException) return Mono.error(e);
                                    log.error("[Repair] 카드 조회 중 예외 발생", e);
                                    return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
                                });

                    } catch (BusinessException e) {
                        return Mono.error(e);
                    } catch (Exception e) {
                        log.error("[Repair] 게임/플레이어 조회 중 예외 발생", e);
                        return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
                    }
                })
                .onErrorResume(e -> {
                    if (e instanceof BusinessException) return Mono.error(e);
                    log.error("[Repair] 전체 처리 중 예외 발생", e);
                    return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
                });
    }

    public Mono<BrokenCardUsedResponse> useBrokenCard(BrokenCardUseRequest request) {
        Long gameId = request.gameId();
        Long playerId = request.playerId();
        int cardId = request.cardId();
        Long targetPlayerId = request.targetPlayerId();

        log.info("[Broken] 카드 사용 요청 - gameId: {}, playerId: {}, targetPlayerId: {}, cardId: {}",
                gameId, playerId, targetPlayerId, cardId);

        return getGameByGameId(gameId)
                .flatMap(game -> {
                    try {
                        Player player = findPlayerByPlayerId(game, playerId);
                        Player targetPlayer = findPlayerByPlayerId(game, targetPlayerId);
                        log.info("[Broken] 플레이어 조회 성공 - playerId: {}, targetPlayerId: {}", playerId, targetPlayerId);

                        return findCardByCardId(cardId)
                                .switchIfEmpty(Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST)))
                                .flatMap(card -> {
                                    try {
                                        log.info("[Broken] 카드 조회 성공 - cardId: {}", cardId);
                                        ActionCard brokenCard = (ActionCard) card;
                                        List<PlayerState> brokenStates = getBrokenStates(brokenCard.getActionCardType());
                                        log.info("[Broken] 부여할 상태 목록 - brokenStates: {}", brokenStates);

                                        boolean alreadyBroken = brokenStates.stream().anyMatch(targetPlayer::hasState);
                                        if (alreadyBroken) {
                                            log.warn("[Broken] 대상 플레이어에게 이미 해당 상태 존재 - targetState: {}, 현재 상태: {}",
                                                    brokenStates, targetPlayer.getState());
                                            return Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST));
                                        }

                                        brokenStates.forEach(targetPlayer::addPlayerState);
                                        player.removeCard(cardId);
                                        log.info("[Broken] 상태 부여 및 카드 제거 완료 - playerId: {}, targetPlayerId: {}, cardId: {}",
                                                playerId, targetPlayerId, cardId);

                                        return saveGameToRedis(game)
                                                .flatMap(success -> {
                                                    if (success) {
                                                        log.info("[Broken] Redis 저장 완료");
                                                        return Mono.just(new BrokenCardUsedResponse(
                                                                gameId, playerId, targetPlayerId, cardId, player.getHand(), new ArrayList<>(targetPlayer.getState())
                                                        ));
                                                    } else {
                                                        log.error("[Broken] Redis 저장 실패");
                                                        return Mono.error(new BusinessException(WsErrorStatus.INTERNAL_ERROR));
                                                    }
                                                })
                                                .onErrorResume(e -> {
                                                    if (e instanceof BusinessException) return Mono.error(e);
                                                    log.error("[Broken] Redis 저장 중 예외 발생", e);
                                                    return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
                                                });

                                    } catch (Exception e) {
                                        log.error("[Broken] 카드 처리 중 예외 발생", e);
                                        return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
                                    }
                                })
                                .onErrorResume(e -> {
                                    if (e instanceof BusinessException) return Mono.error(e);
                                    log.error("[Broken] 카드 조회 중 예외 발생", e);
                                    return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
                                });

                    } catch (BusinessException e) {
                        return Mono.error(e);
                    } catch (Exception e) {
                        log.error("[Broken] 게임/플레이어 조회 중 예외 발생", e);
                        return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
                    }
                })
                .onErrorResume(e -> {
                    if (e instanceof BusinessException) return Mono.error(e);
                    log.error("[Broken] 전체 처리 중 예외 발생", e);
                    return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
                });
    }
}