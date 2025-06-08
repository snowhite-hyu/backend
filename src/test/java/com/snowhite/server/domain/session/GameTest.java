package com.snowhite.server.domain.session;

import com.snowhite.server.domain.enums.PlayerState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    private Game game;
    private Player player1;
    private Player player2;
    private Player player3;

    @BeforeEach
    void setUp() {
        player1 = new Player(1L, "박정은");
        player2 = new Player(2L, "이원태");
        player3 = new Player(3L, "정헌희");

        List<Player> players = new ArrayList<>();
        players.add(player1);
        players.add(player2);
        players.add(player3);

        game = new Game(100L, players, 30);

    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void joinPlayerAndReturnRemain() {
        int remain1 = game.joinPlayerAndReturnRemain(1L);
        assertEquals(2, remain1);

        int remain2 = game.joinPlayerAndReturnRemain(2L);
        assertEquals(1, remain2);

        int remain3 = game.joinPlayerAndReturnRemain(3L);
        assertEquals(0, remain3);

        List<Long> expectedJoined = new ArrayList<>();
        expectedJoined.add(1L);
        expectedJoined.add(2L);
        expectedJoined.add(3L);

        assertTrue(game.getJoinedPlayerIds().containsAll(expectedJoined));
    }

    @Test
    void startNextRoundAndReturnGameFinished() {

        int startCardId = 0;
        List<Integer> destinationCardIds = new ArrayList<>();
        destinationCardIds.add(61);
        destinationCardIds.add(62);
        destinationCardIds.add(63);

        // round 1 시작
        boolean isGameFinished = game.startNextRoundAndReturnGameFinished();
        assertFalse(isGameFinished);

        // round 검사
        assertEquals(1, game.getRound());

        // deck 검사, deck 초기화 이후 player에게 카드를 나눠주기 때문에 전부 합
        assertEquals(67,
                game.getDeckSize() + player1.getHand().size() + player2.getHand().size() + player3.getHand().size());

        // field 검사
        assertEquals(startCardId, game.getField()[3][0][0]);
        assertTrue(destinationCardIds.contains(game.getField()[1][8][0]));
        assertTrue(destinationCardIds.contains(game.getField()[3][8][0]));
        assertTrue(destinationCardIds.contains(game.getField()[5][8][0]));

        // player 상태 검사
        assertTrue(player1.hasState(PlayerState.NORMAL));
        assertFalse(player1.hasState(PlayerState.BROKEN_PICKAXE));
        assertFalse(player1.hasState(PlayerState.BROKEN_MINECART));
        assertFalse(player1.hasState(PlayerState.BROKEN_LANTERN));

        assertTrue(player2.hasState(PlayerState.NORMAL));
        assertFalse(player2.hasState(PlayerState.BROKEN_PICKAXE));
        assertFalse(player2.hasState(PlayerState.BROKEN_MINECART));
        assertFalse(player2.hasState(PlayerState.BROKEN_LANTERN));

        assertTrue(player3.hasState(PlayerState.NORMAL));
        assertFalse(player3.hasState(PlayerState.BROKEN_PICKAXE));
        assertFalse(player3.hasState(PlayerState.BROKEN_MINECART));
        assertFalse(player3.hasState(PlayerState.BROKEN_LANTERN));

        // field 조작
        game.placeCard(3, 0, 5, 0, 0, 0);
        game.placeCard(1, 8, 11, 0, 0, 0);
        game.placeCard(3, 8, 12, 0, 0, 0);
        game.placeCard(5, 8, 13, 0, 0, 0);

        // player 상태 조작
        player1.addPlayerState(PlayerState.BROKEN_PICKAXE);
        player2.addPlayerState(PlayerState.BROKEN_MINECART);
        player3.addPlayerState(PlayerState.BROKEN_LANTERN);

        // deck, player hand 조작
        game.drawAndGiveCardToPlayer(1L);
        game.drawAndGiveCardToPlayer(2L);
        game.drawAndGiveCardToPlayer(3L);

        // round 2 시작
        isGameFinished = game.startNextRoundAndReturnGameFinished();
        assertFalse(isGameFinished);

        // round 검사
        assertEquals(2, game.getRound());

        // deck 재검사
        assertEquals(67,
                game.getDeckSize() + player1.getHand().size() + player2.getHand().size() + player3.getHand().size());

        // field 재검사
        assertEquals(startCardId, game.getField()[3][0][0]);
        assertTrue(destinationCardIds.contains(game.getField()[1][8][0]));
        assertTrue(destinationCardIds.contains(game.getField()[3][8][0]));
        assertTrue(destinationCardIds.contains(game.getField()[5][8][0]));

        // player 상태 재검사
        assertTrue(player1.hasState(PlayerState.NORMAL));
        assertFalse(player1.hasState(PlayerState.BROKEN_PICKAXE));
        assertFalse(player1.hasState(PlayerState.BROKEN_MINECART));
        assertFalse(player1.hasState(PlayerState.BROKEN_LANTERN));

        assertTrue(player2.hasState(PlayerState.NORMAL));
        assertFalse(player2.hasState(PlayerState.BROKEN_PICKAXE));
        assertFalse(player2.hasState(PlayerState.BROKEN_MINECART));
        assertFalse(player2.hasState(PlayerState.BROKEN_LANTERN));

        assertTrue(player3.hasState(PlayerState.NORMAL));
        assertFalse(player3.hasState(PlayerState.BROKEN_PICKAXE));
        assertFalse(player3.hasState(PlayerState.BROKEN_MINECART));
        assertFalse(player3.hasState(PlayerState.BROKEN_LANTERN));

        // round 3 시작
        isGameFinished = game.startNextRoundAndReturnGameFinished();
        assertFalse(isGameFinished);

        // round 검사
        assertEquals(3, game.getRound());

        // 게임 종료 검사
        isGameFinished = game.startNextRoundAndReturnGameFinished();
        assertTrue(isGameFinished);

    }
}