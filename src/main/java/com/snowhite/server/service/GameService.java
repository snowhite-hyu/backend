package com.snowhite.server.service;

import com.snowhite.server.domain.entity.ActionCard;
import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.entity.PathCard;
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
import com.snowhite.server.websocket.dto.request.ActionCardUseRequest;
import com.snowhite.server.websocket.dto.response.ActionCardUsedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final String GAME_PREFIX = "game:";
    private static final String CARD_PREFIX = "card:";

    private final ReactiveRedisTemplate<String, Game> reactiveRedisTemplateForGame;
    private final ReactiveRedisTemplate<String, Card> reactiveRedisTemplateForCard;

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

    private Mono<Card> findCardByCardId(Integer cardId) {
        return getAllCardsFromRedis()
                .filter(card -> card.getId().equals(cardId))
                .next();
    }

    private Mono<Boolean> handlePathCard(PathCard pathCard) {
        return Mono.fromCallable(() -> {
            // TODO: 굴 카드 관련 로직
            return true;
        });
    }

    private Mono<Boolean> saveGameToRedisById(Game game) {
        return reactiveRedisTemplateForGame.opsForValue()
                .set(GAME_PREFIX + game.getGameId(), game);
    }

    private Mono<ActionCardUsedResponse> useRockfallCard(Game game, Player player, ActionCard actionCard, ActionCardUseRequest request, long gameId, long playerId) {
        try {
            if (request.locationX() == null || request.locationY() == null) {
                return Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST));
            }

            int locationX = request.locationX();
            int locationY = request.locationY();

            if (!game.isPossibleLocationToGetCard(locationX, locationY)) {
                return Mono.error(new BusinessException(WsErrorStatus.CANNOT_USE_CARD));
            }
            game.removeCard(locationX, locationY);
            player.removeCard(actionCard.getId());

            return saveGameToRedisById(game)
                    .flatMap(success -> {
                        if (success) {
                            return Mono.just(new ActionCardUsedResponse.Builder()
                                    .gameId(gameId)
                                            .message("success")
                                            .actionCardId(actionCard.getId())
                                            .usePlayerId(playerId)
                                            .usePlayerCards(player.getHand())
                                            .field(game.getField())
                                    .build()
                            );
                        } else {
                            return Mono.error(new BusinessException(WsErrorStatus.INTERNAL_ERROR));
                        }
                    });


        } catch (Exception e) {
            return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
        }
    }

    private Mono<ActionCardUsedResponse> useMapCard(Game game, Player player, ActionCard actionCard, ActionCardUseRequest request, long gameId, long playerId) {
        try {
            if (request.locationX() == null || request.locationY() == null) {
                return Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST));
            }
            int locationX = request.locationX();
            int locationY = request.locationY();

            if (!game.isFlipped(locationX, locationY)) {
                return Mono.error(new BusinessException(WsErrorStatus.CANNOT_USE_CARD));
            }
            player.removeCard(actionCard.getId());
            return saveGameToRedisById(game)
                    .flatMap(success -> {
                        if(success) {
                            return Mono.just(new ActionCardUsedResponse.Builder()
                                    .gameId(gameId)
                                    .message("success")
                                    .actionCardId(actionCard.getId())
                                    .usePlayerId(playerId)
                                    .usePlayerCards(player.getHand())
                                    .build()
                            );
                        } else {
                            return Mono.error(new BusinessException(WsErrorStatus.INTERNAL_ERROR));
                        }
                    });
        } catch (Exception e) {
            return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
        }
    }

    private Mono<ActionCardUsedResponse> handleRepairOrBrokenCards(Game game, Player player, Player targetPlayer, ActionCard actionCard, ActionCardUseRequest request, long gameId, long playerId) {
        try {
            List<PlayerState> repairState = getRepairStates(actionCard.getActionCardType());
            List<PlayerState> brokenState = getBrokenStates(actionCard.getActionCardType());

            if (!repairState.isEmpty()) {
                return useRepairCard(game, player, targetPlayer, actionCard, request.targetRepairState(), gameId, playerId, repairState);
            } else if (!brokenState.isEmpty()) {
                return useBrokenCard(game, player, targetPlayer, actionCard, gameId, playerId, brokenState);
            }
            return Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST));
        } catch (Exception e) {
            return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
        }
    }

    private Mono<ActionCardUsedResponse> useRepairCard(Game game, Player player, Player targetPlayer, ActionCard actionCard, PlayerState targetState, long gameId, long playerId, List<PlayerState> repairState) {
        try {
            if (repairState.stream().noneMatch(targetPlayer::hasState) || !repairState.contains(targetState)) {
                return Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST));
            }
            targetPlayer.removePlayerState(targetState);
            player.removeCard(actionCard.getId());

            return saveGameToRedisById(game)
                    .flatMap(success -> {
                        if(success) {
                            return Mono.just(new ActionCardUsedResponse.Builder()
                                            .gameId(gameId)
                                            .message("success")
                                            .actionCardId(actionCard.getId())
                                            .targetPlayerId(targetPlayer.getPlayerId())
                                            .targetPlayerState(List.of(targetState))
                                            .usePlayerId(playerId)
                                            .usePlayerCards(player.getHand())
                                            .build()
                            );
                        } else {
                            return Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST));
                        }
                    });
        } catch (Exception e) {
            return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
        }
    }

    private Mono<ActionCardUsedResponse> useBrokenCard(Game game, Player player, Player targetPlayer, ActionCard actionCard, long gameId, long playerId, List<PlayerState> brokenState) {
        try {
            if (brokenState.stream().anyMatch(targetPlayer::hasState)) {
                return Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST));
            }
            brokenState.forEach(targetPlayer::addPlayerState);
            player.removeCard(actionCard.getId());

            return saveGameToRedisById(game)
                    .flatMap(success -> {
                        if(success) {
                            return Mono.just(new ActionCardUsedResponse.Builder()
                                    .gameId(gameId)
                                    .message("success")
                                    .actionCardId(actionCard.getId())
                                    .targetPlayerId(targetPlayer.getPlayerId())
                                    .targetPlayerState(new ArrayList<>(targetPlayer.getState()))
                                    .usePlayerId(playerId)
                                    .usePlayerCards(player.getHand())
                                    .build()
                            );
                        } else {
                            return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
                        }
                    });
        } catch (Exception e) {
            return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
        }
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

    public Mono<ActionCardUsedResponse> useActionCard(Long gamId, ActionCardUseRequest request) {
        try {

            long gameId = gamId;
            long playerId = request.usePlayerId();
            int actionCardId = request.cardId();

            return reactiveRedisTemplateForGame.opsForValue().get(GAME_PREFIX + gameId)
                    .flatMap(game -> {
                        Player player = findPlayerByPlayerId(game, playerId);
                        Player targetPlayer = findPlayerByPlayerId(game, request.targetPlayerId());

                        if (player == null) {
                            return Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST));
                        }
                        if (!player.hasCard(actionCardId)) {
                            return Mono.error(new BusinessException(WsErrorStatus.CANNOT_USE_CARD));
                        }
                        return findCardByCardId(actionCardId)
                                .flatMap(card -> {
                                    if (!(card instanceof ActionCard)) {
                                        return Mono.error(new BusinessException(WsErrorStatus.BAD_REQUEST));
                                    }

                                    ActionCard actionCard = (ActionCard) card;

                                    switch (actionCard.getActionCardType()) {
                                        case ROCKFALL -> {
                                            return useRockfallCard(game, player, actionCard, request, gameId, playerId);
                                        }
                                        case MAP -> {
                                            return useMapCard(game, player, actionCard, request, gameId, playerId);
                                        }
                                        default -> {
                                            return handleRepairOrBrokenCards(game, player, targetPlayer, actionCard, request, gameId, playerId);
                                        }
                                    }
                                });
                    });
        } catch (Exception e) {
            return Mono.error(new WebSocketException(WsErrorStatus.INTERNAL_ERROR.getErrorReason()));
        }
    }
}