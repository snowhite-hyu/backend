package com.snowhite.server.service;

import com.snowhite.server.domain.entity.ActionCard;
import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.enums.ActionCardType;
import com.snowhite.server.domain.enums.PlayerState;
import com.snowhite.server.domain.session.Game;
import com.snowhite.server.domain.session.Player;
import com.snowhite.server.payload.exception.BusinessException;
import com.snowhite.server.websocket.dto.request.BrokenCardUseRequest;
import com.snowhite.server.websocket.dto.request.MapCardUseRequest;
import com.snowhite.server.websocket.dto.request.RepairCardUseRequest;
import com.snowhite.server.websocket.dto.request.RockfallCardUseRequest;
import com.snowhite.server.websocket.dto.response.BrokenCardUsedResponse;
import com.snowhite.server.websocket.dto.response.MapCardUsedResponse;
import com.snowhite.server.websocket.dto.response.RepairCardUsedResponse;
import com.snowhite.server.websocket.dto.response.RockfallCardUsedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureWebTestClient
public class ActionCardServiceTest {

    private ReactiveRedisTemplate<String, Game> reactiveRedisTemplateForGame;
    private ReactiveRedisTemplate<String, Card> reactiveRedisTemplateForCard;
    private ReactiveValueOperations<String, Game> valueOperationsForGame;
    private ReactiveValueOperations<String, Card> valueOperationsForCard;

    @InjectMocks
    private GameService gameService;

    private static String GAME_PREFIX = "game:";
    private static String CARD_PREFIX = "card:";
    private static long GAME_ID;
    private static int TURN_TIME = 10;
    private static Player player1 = new Player(10L, "player1");
    private static Player player2 = new Player(20L, "player2");
    private static Player player3 = new Player(30L, "player3");
    private static Player player4 = new Player(40L, "player4");
    private static Player player5 = new Player(50L, "player5");

    private Game game;

    private int actionCardId;

    @BeforeEach
    void setup() {
        reactiveRedisTemplateForGame = Mockito.mock(ReactiveRedisTemplate.class);
        valueOperationsForGame = Mockito.mock(ReactiveValueOperations.class);
        reactiveRedisTemplateForCard = Mockito.mock(ReactiveRedisTemplate.class);
        valueOperationsForCard = Mockito.mock(ReactiveValueOperations.class);

        when(reactiveRedisTemplateForGame.opsForValue()).thenReturn(valueOperationsForGame);
        when(reactiveRedisTemplateForCard.opsForValue()).thenReturn(valueOperationsForCard);
        gameService = new GameService(reactiveRedisTemplateForGame, reactiveRedisTemplateForCard);
    }

    void rockFallCardSetup() {
        GAME_ID = 1L;
        List<Player> players = List.of(player1, player2, player3, player4, player5);
        game = new Game(GAME_ID, players, TURN_TIME);
        game.clearField();
        game.getField()[5][5][0] = 10;
        actionCardId = 111; // rockfall card id
        player1.addCardToHand(actionCardId);
        when(valueOperationsForGame.get(eq(GAME_PREFIX + GAME_ID))).thenReturn(Mono.just(game));
        when(valueOperationsForGame.set(eq(GAME_PREFIX + GAME_ID), any(Game.class))).thenReturn(Mono.just(true));
    }

    @Test
    void rockfallCardSuccessTest() {
        rockFallCardSetup();

        RockfallCardUseRequest request = new RockfallCardUseRequest(
                GAME_ID, player1.getPlayerId(), actionCardId, 5,5);
        Mono<RockfallCardUsedResponse> response = gameService.useRockfallCard(request);

        StepVerifier.create(response)
                .assertNext(res -> {
                    assertFalse(res.playerHand().contains(actionCardId));
                    assertEquals(-1, game.getField()[5][5][0]);
                })
                .verifyComplete();
    }
    // 1. 빈 필드(-1) 위치에 대해 rockfall 시도
    @Test
    void rockfallFail_whenTargetFieldIsEmpty() {
        rockFallCardSetup();
        game.getField()[5][5][0] = -1;

        RockfallCardUseRequest request = new RockfallCardUseRequest(
                GAME_ID, player1.getPlayerId(), actionCardId, 5, 5
        );
        Mono<RockfallCardUsedResponse> response = gameService.useRockfallCard(request);

        StepVerifier.create(response)
                .expectError(BusinessException.class)
                .verify();

        assertTrue(player1.getHand().contains(actionCardId)); // player 패가 변하지 않음
    }
    // 2. 시작 카드 위치에 대해 rockfall 시도
    @Test
    void rockfallFail_whenTargetIsStartCard() {
        rockFallCardSetup();

        int startRow = 3; int startCol = 0;

        RockfallCardUseRequest request = new RockfallCardUseRequest(
                GAME_ID, player1.getPlayerId(), actionCardId, startRow, startCol
        );
        Mono<RockfallCardUsedResponse> response = gameService.useRockfallCard(request);

        StepVerifier.create(response)
                .expectError(BusinessException.class)
                .verify();

        assertEquals(10, game.getField()[5][5][0]); // field가 변하지 않음
        assertTrue(player1.getHand().contains(actionCardId)); // player 패가 변하지 않음
    }

    // 3. 목적지 카드 위치에 대해 rockfall 시도
    @Test
    void rockfallFail_whenTargetIsDestCard() {
        rockFallCardSetup();
        int[][] destPositions = {
                {1, 8},
                {3, 8},
                {5, 8}
        };

        for (int[] pos : destPositions) {
            int row = pos[0];
            int col = pos[1];

            RockfallCardUseRequest request = new RockfallCardUseRequest(
                    GAME_ID, player1.getPlayerId(), actionCardId, row, col
            );
            Mono<RockfallCardUsedResponse> response = gameService.useRockfallCard(request);

            StepVerifier.create(response)
                    .expectError(BusinessException.class)
                    .verify();

            assertEquals(10, game.getField()[5][5][0]); // field가 변하지 않음
            assertTrue(player1.getHand().contains(actionCardId)); // player 패가 변하지 않음
        }
    }

    void mapCardSetup() {
        GAME_ID = 2L;
        List<Player> players = List.of(player1, player2, player3, player4, player5);
        game = new Game(GAME_ID, players, TURN_TIME);
        game.clearField();
        // dest card id: 61-`63
        game.getField()[1][8][0] = 61; game.getField()[1][8][1] = 0;
        game.getField()[3][8][0] = 62; game.getField()[3][8][1] = 0;
        game.getField()[5][8][0] = 63; game.getField()[5][8][1] = 0;
        actionCardId = 107; // map card id
        player1.addCardToHand(actionCardId);
        when(valueOperationsForGame.get(eq(GAME_PREFIX + GAME_ID))).thenReturn(Mono.just(game));
        when(valueOperationsForGame.set(eq(GAME_PREFIX + GAME_ID), any(Game.class))).thenReturn(Mono.just(true));
    }

    @Test
    void mapCardSuccessTest() {
        mapCardSetup();

        MapCardUseRequest request = new MapCardUseRequest(
                GAME_ID, player1.getPlayerId(), actionCardId, 1, 8
        );
        Mono<MapCardUsedResponse> response = gameService.useMapCard(request);
        StepVerifier.create(response)
                .assertNext(res -> {
                    assertFalse(res.playerHand().contains(actionCardId));
                    assertEquals(61, res.cardId());
                })
                .verifyComplete();
    }

    // 1. 목적지 카드가 아닌 위치에 map 시도
    @Test
    void mapFail_whenTargetIsEmptyField(){
        mapCardSetup();
        MapCardUseRequest request = new MapCardUseRequest(
                GAME_ID, player1.getPlayerId(), actionCardId, 5, 5
        );
        Mono<MapCardUsedResponse> response = gameService.useMapCard(request);

        StepVerifier.create(response)
                .expectError(BusinessException.class)
                .verify();

        assertTrue(player1.getHand().contains(actionCardId)); // player 패가 변하지 않음
    }

    void repairCardSetup() {
        GAME_ID = 3L;
        List<Player> players = List.of(player1, player2, player3, player4, player5);
        game = new Game(GAME_ID, players, TURN_TIME);
        game.clearField();
        actionCardId = 104; // repair card id: REPAIR_PICKAXE_AND_LANTERN
        player1.addCardToHand(actionCardId);
        when(valueOperationsForGame.get(eq(GAME_PREFIX + GAME_ID))).thenReturn(Mono.just(game));
        when(valueOperationsForGame.set(eq(GAME_PREFIX + GAME_ID), any(Game.class))).thenReturn(Mono.just(true));

        Card repairCard = new ActionCard(actionCardId, "tool - pickaxe, lantern", ActionCardType.REPAIR_PICKAXE_AND_LANTERN);
        when(valueOperationsForCard.get(CARD_PREFIX+actionCardId)).thenReturn(Mono.just(repairCard));
    }

    @Test
    void repairCardSuccessTest() {
        repairCardSetup();
        player2.addPlayerState(PlayerState.BROKEN_PICKAXE);
        player2.addPlayerState(PlayerState.BROKEN_LANTERN);
        RepairCardUseRequest request = new RepairCardUseRequest(
                GAME_ID, player1.getPlayerId(), player2.getPlayerId(), actionCardId, PlayerState.BROKEN_PICKAXE
        );
        Mono<RepairCardUsedResponse> response = gameService.useRepairCard(request);

        StepVerifier.create(response)
                .assertNext(res -> {
                    assertFalse(res.playerHand().contains(actionCardId));
                    assertFalse(res.targetPlayerState().contains(PlayerState.BROKEN_PICKAXE));
                    assertTrue(res.targetPlayerState().contains(PlayerState.BROKEN_LANTERN));
                })
                .verifyComplete();
    }

    // 1. tragetPlayer에 수리하고자 하는 brokenState가 없는 경우
    @Test
    void repairFail_whenTargetHasNoBrokenState() {
        repairCardSetup();

        player2.addPlayerState(PlayerState.NORMAL);
        RepairCardUseRequest request = new RepairCardUseRequest(
                GAME_ID, player1.getPlayerId(), player2.getPlayerId(), actionCardId, PlayerState.BROKEN_PICKAXE
        );
        Mono<RepairCardUsedResponse> response = gameService.useRepairCard(request);

        StepVerifier.create(response)
                .expectError(BusinessException.class)
                .verify();

        assertTrue(player1.getHand().contains(actionCardId)); // player 패가 변하지 않음
        // target player의 상태가 변하지 않음
        assertTrue(player2.getState().contains(PlayerState.NORMAL));

    }
    // 2. repair card 종류와는 다른 targetState를 지정
    @Test
    void repairFail_whenTargetStateDoesNotMatchCardType() {
        repairCardSetup();

        RepairCardUseRequest request = new RepairCardUseRequest(
                GAME_ID, player1.getPlayerId(), player2.getPlayerId(), actionCardId, PlayerState.BROKEN_MINECART
        );
        Mono<RepairCardUsedResponse> response = gameService.useRepairCard(request);

        StepVerifier.create(response)
                .expectError(BusinessException.class)
                .verify();

        assertTrue(player1.getHand().contains(actionCardId)); // player 패가 변하지 않음
        // target player의 상태가 변하지 않음
        assertTrue(player2.getState().contains(PlayerState.BROKEN_PICKAXE));
        assertFalse(player2.getState().contains(PlayerState.NORMAL));
    }

    void brokenCardSetup() {
        GAME_ID = 4L;
        List<Player> players = List.of(player1, player2, player3, player4, player5);
        game = new Game(GAME_ID, players, TURN_TIME);
        game.clearField();
        actionCardId = 109; // broken card id: BROKEN_PICKAXE
        player1.addCardToHand(actionCardId);
        player2.addPlayerState(PlayerState.BROKEN_LANTERN);
        when(valueOperationsForGame.get(eq(GAME_PREFIX + GAME_ID))).thenReturn(Mono.just(game));
        when(valueOperationsForGame.set(eq(GAME_PREFIX + GAME_ID), any(Game.class))).thenReturn(Mono.just(true));

        Card brokenCard = new ActionCard(actionCardId, "broken - pickaxe", ActionCardType.BROKEN_PICKAXE);
        when(valueOperationsForCard.get(CARD_PREFIX+actionCardId)).thenReturn(Mono.just(brokenCard));
    }

    @Test
    void brokenCardSuccessTest() {
        brokenCardSetup();

        BrokenCardUseRequest request = new BrokenCardUseRequest(
                GAME_ID, player1.getPlayerId(), player2.getPlayerId(), actionCardId
        );
        Mono<BrokenCardUsedResponse> response = gameService.useBrokenCard(request);

        StepVerifier.create(response)
                .assertNext(res -> {
                    assertFalse(res.playerHand().contains(actionCardId));
                    assertTrue(res.targetPlayerState().contains(PlayerState.BROKEN_PICKAXE));
                    assertTrue(res.targetPlayerState().contains(PlayerState.BROKEN_LANTERN));
                })
                .verifyComplete();
    }
    
    // 1. targetPlayer가 이미 해당 playerState를 가지고 있는 경우
    @Test
    void brokenFail_whenTargetHasSameBrokenState() {
        brokenCardSetup();
        player2.addPlayerState(PlayerState.BROKEN_PICKAXE);

        BrokenCardUseRequest request = new BrokenCardUseRequest(
                GAME_ID, player1.getPlayerId(), player2.getPlayerId(), actionCardId
        );

        Mono<BrokenCardUsedResponse> response = gameService.useBrokenCard(request);

        StepVerifier.create(response)
                .expectError(BusinessException.class)
                .verify();

        assertTrue(player1.getHand().contains(actionCardId)); // player 패가 변하지 않음
        // target player의 상태가 변하지 않음
        assertTrue(player2.getState().contains(PlayerState.BROKEN_PICKAXE));
        assertTrue(player2.getState().contains(PlayerState.BROKEN_LANTERN));
    }
    
}
