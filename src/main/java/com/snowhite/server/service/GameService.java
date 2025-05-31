package com.snowhite.server.service;

import com.snowhite.server.domain.entity.ActionCard;
import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.entity.PathCard;
import com.snowhite.server.domain.enums.PlayerState;
import com.snowhite.server.domain.enums.ActionCardType;
import com.snowhite.server.domain.session.Game;
import com.snowhite.server.domain.session.Player;
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

@Service
@RequiredArgsConstructor
public class GameService {

    private static final String GAME_PREFIX = "game:";
    private static final String CARD_PREFIX = "card:";

    private final ReactiveRedisTemplate<String, Game> reactiveRedisTemplateForGame;
    private final ReactiveRedisTemplate<String, Card> reactiveRedisTemplateForCard;

    // game에 player를 join시킨 후 남은 player 수 리턴
    public Mono<Integer> joinPlayer(Long gameId, Long playerId) {

        return reactiveRedisTemplateForGame.opsForValue().get(GAME_PREFIX + gameId)
                .map(game -> {
                    game.getJoinedPlayerIds().add(playerId);
                    reactiveRedisTemplateForGame.opsForValue().set(GAME_PREFIX + gameId, game);
                    int remain = game.getPlayers().size() - game.getJoinedPlayerIds().size();
                    return remain;
                });
    }

    // 해당 game에 모든 player가 join했는지 확인
    public Mono<Boolean> verifyAllJoined(Long gameId) {
        return reactiveRedisTemplateForGame.opsForValue().get(GAME_PREFIX + gameId)
                .map(game -> game.getJoinedPlayerIds().size() == game.getPlayers().size());
    }

    // 새로운 round 시작을 위한 모든 field 초기화
    public Mono<Game> setupGameForNewRound(Long gameId) {

        return reactiveRedisTemplateForGame.opsForValue().get(GAME_PREFIX + gameId)
                .flatMap(game -> {
                    game.incrementRoundAndChangeGameState();
                    game.setNewDeck();
                    game.shufflePlayers();
                    game.nextTurn();
                    initializeNewField(game);
                    initializePlayerRole(game);
                    initializeAllPlayerHands(game);
                    return reactiveRedisTemplateForGame.opsForValue()
                            .set(GAME_PREFIX + gameId, game)
                            .thenReturn(game);
                });
    }

    public Mono<Game> getGameByGameId(Long gameId) {
        return reactiveRedisTemplateForGame.opsForValue().get(GAME_PREFIX + gameId);
    }

    public Mono<Player> findPlayerByGameIdAndPlayerId(Long gameId, Long playerId) {
        return getGameByGameId(gameId)
                .map(game -> game.findPlayer(playerId).get());
    }

    // 출발지, 목적지 카드 세팅
    public void initializeNewField(Game game) {
        game.clearField();
        List<Integer> cardIds = new ArrayList<>();
        cardIds.add(61);
        cardIds.add(62);
        cardIds.add(63);
        Collections.shuffle(cardIds);

        game.placeCard(3, 0, 0, 0);
        game.placeCard(1, 8, cardIds.get(0), 0);
        game.placeCard(3, 8, cardIds.get(1), 0);
        game.placeCard(5, 8, cardIds.get(2), 0);

    }

    // 모든 player의 손패 초기화
    private void initializeAllPlayerHands(Game game) {
        int playerCount = game.getPlayerCount();
        int cardNumber = 0;

        switch (playerCount) {
            case 3, 4, 5:
                cardNumber = 6;
                break;
            case 6, 7:
                cardNumber = 5;
                break;
            case 8, 9, 10:
                cardNumber = 4;
                break;
            default:
                break;
        }

        List<Player> players = game.getPlayers();
        for (Player player : players) {
            player.clearHand();
            for (int i = 0; i < cardNumber; i++) {
                game.drawAndGiveCardToPlayer(player.getPlayerId());
            }
        }
    }

    // 모든 player의 역할 초기화
    private void initializePlayerRole(Game game) {
        int playerCount = game.getPlayerCount();
        int dwarf = 0;
        int saboteur = 0;

        switch (playerCount) {
            case 3:
                dwarf = 3;
                saboteur = 1;
                break;
            case 4:
                dwarf = 4;
                saboteur = 1;
                break;
            case 5:
                dwarf = 4;
                saboteur = 2;
                break;
            case 6:
                dwarf = 5;
                saboteur = 2;
                break;
            case 7:
                dwarf = 5;
                saboteur = 3;
                break;
            case 8:
                dwarf = 6;
                saboteur = 3;
                break;
            case 9:
                dwarf = 7;
                saboteur = 3;
                break;
            case 10:
                dwarf = 7;
                saboteur = 4;
                break;
            default:
                break;
        }
        game.distributeRoles(dwarf, saboteur);
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
                                            .usePlayerCards(player.getCards())
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
                                    .usePlayerCards(player.getCards())
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
                                            .usePlayerCards(player.getCards())
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
                                    .usePlayerCards(player.getCards())
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