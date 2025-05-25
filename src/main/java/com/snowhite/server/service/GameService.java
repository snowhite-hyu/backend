package com.snowhite.server.service;

import com.snowhite.server.domain.entity.ActionCard;
import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.entity.PathCard;
import com.snowhite.server.domain.enums.PlayerState;
import com.snowhite.server.domain.session.Game;
import com.snowhite.server.domain.session.Player;
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
import java.util.Objects;

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

    private Player findPlayerById(Game game, Long playerId) {
        return game.getPlayers().stream()
                .filter(player -> player.getPlayerId() == playerId)
                .findFirst()
                .orElse(null);
    }

    private Mono<Card> findCardById(Integer cardId) {
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

    private Mono<ActionCardUsedResponse> saveGameToRedisById(Game game, ActionCardUsedResponse response) {
        return reactiveRedisTemplateForGame.opsForValue()
                .set(GAME_PREFIX + response.gameId(), game)
                .thenReturn(ActionCardUsedResponse.of(response.message(), response.gameId(), response.changedPlayerCard(), response.changedTargetPlayerState()));
    }

    public Mono<ActionCardUsedResponse> useActionCard(Long gamId, ActionCardUseRequest request) {

        long gameId = gamId;
        long playerId = request.playerId();
        int actionCardId = request.cardId();

        return reactiveRedisTemplateForGame.opsForValue().get(GAME_PREFIX + gameId)
                .flatMap(game -> {
                    Player player = findPlayerById(game, playerId);
                    Player targetPlayer = findPlayerById(game, request.targetPlayerId());

                    if (player == null || targetPlayer == null) { return Mono.just(ActionCardUsedResponse.of("player 정보가 유효하지 않습니다.", gameId,null, null, null)); }
                    if (!player.hasCard(actionCardId)) { return Mono.just(ActionCardUsedResponse.of("해당 action card를 소유하고 있지 않습니다.", gameId,null, null, null)); }
                    return findCardById(actionCardId)
                            .flatMap(card -> {
                                if (!(card instanceof ActionCard)) { return Mono.just(ActionCardUsedResponse.of("해당 카드는 action card가 아닙니다.", gameId,null, null, null)); }

                                ActionCard actionCard = (ActionCard) card;

                                switch(actionCard.getActionCardType()) {
                                    case ROCKFALL -> {
                                        if (request.locationX() == null || request.locationY() == null) { return Mono.just(ActionCardUsedResponse.of("request에 field의 x, y 값이 없습니다.", gameId,null, null, null)); }
                                        int locationX = request.locationX();
                                        int locationY = request.locationY();

                                        if (!game.isPossibleLocationToGetCard(locationX, locationY))
                                            { return Mono.just(ActionCardUsedResponse.of("해당 위치에 사용 불가합니다.", gameId,null, null, null)); }

                                        // 사용한 카드 제거
                                        player.removeCard(actionCardId);
                                        return saveGameToRedisById(game, ActionCardUsedResponse.of("success", gameId, actionCardId, null, game.getField()));
                                    }
                                    case MAP -> {
                                        if (request.locationX() == null || request.locationY() == null) { return Mono.just(ActionCardUsedResponse.of("request에 field의 x, y 값이 없습니다.", gameId,null, null, null)); }
                                        int locationX = request.locationX();
                                        int locationY = request.locationY();

                                        if ( !game.isFlipped(locationX, locationY)) { return Mono.just(ActionCardUsedResponse.of("이미 공개된 목적지 카드입니다.", gameId,null, null, null)); }

                                        // 사용한 카드 제거
                                        player.removeCard(actionCardId);
                                        return saveGameToRedisById(game, ActionCardUsedResponse.of("success", gameId, actionCardId,null, null));
                                    }
                                    default -> {
                                        List<PlayerState> repairState = new ArrayList<>(); // repair 대상이 될 수 있는 state
                                        List<PlayerState> brokenState = new ArrayList<>(); // broken 대상이 될 수 있는 state
                                        switch(actionCard.getActionCardType()) {
                                            case REPAIR_PICKAXE -> {
                                                repairState.add(PlayerState.BROKEN_PICKAXE);
                                            }
                                            case REPAIR_LANTERN -> {
                                                repairState.add(PlayerState.BROKEN_LANTERN);
                                            }
                                            case REPAIR_MINECART -> {
                                                repairState.add(PlayerState.BROKEN_MINCART);
                                            }
                                            case REPAIR_PICKAXE_AND_LANTERN -> {
                                                repairState.add(PlayerState.BROKEN_PICKAXE);
                                                repairState.add(PlayerState.BROKEN_LANTERN);
                                            }
                                            case REPAIR_PICKAXE_AND_MINECART -> {
                                                repairState.add(PlayerState.BROKEN_PICKAXE);
                                                repairState.add(PlayerState.BROKEN_MINCART);
                                            }
                                            case REPAIR_LANTERN_MINECART -> {
                                                repairState.add(PlayerState.BROKEN_LANTERN);
                                                repairState.add(PlayerState.BROKEN_MINCART);
                                            }
                                            default -> {
                                                switch(actionCard.getActionCardType()) {
                                                    case BROKEN_PICKAXE -> {
                                                        brokenState.add(PlayerState.BROKEN_PICKAXE);
                                                    }
                                                    case BROKEN_LANTERN -> {
                                                        brokenState.add(PlayerState.BROKEN_LANTERN);
                                                    }
                                                    case BROKEN_MINECART -> {
                                                        brokenState.add(PlayerState.BROKEN_MINCART);
                                                    }
                                                    default -> {
                                                        return Mono.just(ActionCardUsedResponse.of("사용 가능한 action type이 아닙니다.", gameId ,null, null, null));
                                                    }
                                                }
                                                // broken
                                                boolean canBroken = brokenState.stream().noneMatch(targetPlayer::hasState);
                                                if (!canBroken) { return Mono.just(ActionCardUsedResponse.of("target player에게 해당 카드 사용이 불가합니다.", gameId,null, null, null));}
                                                brokenState.forEach(targetPlayer::addPlayerState); // broken 상태 추가
                                                // 사용한 카드 제거
                                                player.removeCard(actionCardId);
                                                return saveGameToRedisById(game, ActionCardUsedResponse.of("success", gameId, actionCardId, brokenState, null));
                                            }
                                        }
                                        // repair
                                        PlayerState targetState = request.targetRepairState();
                                        // targetPlayer에게 매칭되는 broken state가 있는지 && 해당 action card로 수리 가능한지
                                        boolean canRepair = repairState.stream().anyMatch(targetPlayer::hasState) && repairState.contains(targetState); 
                                        if (!canRepair) { return Mono.just(ActionCardUsedResponse.of("target player에게 해당 카드 사용이 불가합니다.", gameId,null, null, null)); }
                                        targetPlayer.removePlayerState(targetState); // repair = broken 상태 제거
                                        // 사용한 카드 제거
                                        player.removeCard(actionCardId);
                                        return saveGameToRedisById(game, ActionCardUsedResponse.of("success", gameId, actionCardId, List.of(targetState), null));
                                    }

                                }
                            });
                })
                .switchIfEmpty(Mono.just(ActionCardUsedResponse.of("game이 존재하지 않습니다.", gameId,null, null, null)))
                .onErrorResume(e -> Mono.just(ActionCardUsedResponse.of("error 발생", gameId,null, null, null)));
    }
}