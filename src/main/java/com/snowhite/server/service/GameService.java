package com.snowhite.server.service;

import com.snowhite.server.domain.entity.ActionCard;
import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.entity.PathCard;
import com.snowhite.server.domain.enums.ActionCardType;
import com.snowhite.server.domain.enums.CardType;
import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.enums.PlayerState;
import com.snowhite.server.domain.session.Game;
import com.snowhite.server.domain.session.Player;
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
            // 경로 카드 관련 로직 구현
            return true;
        });
    }

    public Mono<Boolean> useActionCard(Long gameId, Long playerId, Long targetPlayerId, Integer actionCardId) {
        return reactiveRedisTemplateForGame.opsForValue().get(GAME_PREFIX + gameId)
                .flatMap(game -> {
                    // 1. player, targetPlayer 조회
                    Player player = findPlayerById(game, playerId);
                    Player targetPlayer = findPlayerById(game, targetPlayerId);

                    if (player == null || targetPlayer == null) {
                        return Mono.just(false);
                    }

                    // 2. player의 손패에 해당 카드가 있는지 확인
                    if (!player.hasCard(actionCardId)) {
                        return Mono.just(false);
                    }

                    // 3. 카드 조회 및 처리
                    return findCardById(actionCardId)
                            .flatMap(c -> {
                                if (!(c instanceof ActionCard)) {
                                    return Mono.just(false); // 액션 카드 아님
                                }
                                ActionCard actionCard = (ActionCard) c;

                                // 4. 액션 타입별 상태
                                List<PlayerState> repairState = new ArrayList<>();
                                List<PlayerState> brokenState = new ArrayList<>();
                                switch (actionCard.getActionCardType()) {
                                    case REPAIR_PICKAXE: // enum 이름 중복 제거
                                        repairState.add(PlayerState.BROKEN_PICKAXE);
                                        break;
                                    case REPAIR_LANTERN:
                                        repairState.add(PlayerState.BROKEN_LANTERN);
                                        break;

                                    case REPAIR_MINECART:
                                        repairState.add(PlayerState.BROKEN_MINCART);
                                        break;
                                    case REPAIR_LANTERN_MINECART:
                                        repairState.add(PlayerState.BROKEN_LANTERN);
                                        repairState.add(PlayerState.BROKEN_MINCART);
                                        break;
                                    case REPAIR_PICKAXE_AND_LANTERN:
                                        repairState.add(PlayerState.BROKEN_PICKAXE);
                                        repairState.add(PlayerState.BROKEN_LANTERN);
                                        break;
                                    case REPAIR_PICKAXE_AND_MINECART:
                                        repairState.add(PlayerState.BROKEN_PICKAXE);
                                        repairState.add(PlayerState.BROKEN_MINCART);
                                        break;
                                    case BROKEN_PICKAXE:
                                        brokenState.add(PlayerState.BROKEN_PICKAXE);
                                        break;
                                    case BROKEN_LANTERN:
                                        brokenState.add(PlayerState.BROKEN_PICKAXE);
                                        break;
                                    case BROKEN_MINECART:
                                        brokenState.add(PlayerState.BROKEN_MINCART);
                                        break;
                                    // map, rockFall 추가 필요
                                    default:
                                        return Mono.just(false);
                                }

                                // 5. state에 따라 사용 가능한지 검증
                                boolean canRepair = repairState.stream().allMatch(targetPlayer::hasState);
                                boolean canBreak = brokenState.stream().noneMatch(targetPlayer::hasState);
                                if (!canRepair || !canBreak) {
                                    return Mono.just(false);
                                }

                                // 6. 상태 변경 적용
                                repairState.forEach(targetPlayer::removePlayerState);
                                brokenState.forEach(targetPlayer::addPlayerState);
                                player.removeCard(actionCardId);

                                // 7. 게임 상태 저장
                                return reactiveRedisTemplateForGame.opsForValue()
                                        .set(GAME_PREFIX + gameId, game)
                                        .thenReturn(true);
                            });
                })
                .defaultIfEmpty(false)
                .onErrorReturn(false);
    }
}
