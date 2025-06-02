package com.snowhite.server.service;

import com.snowhite.server.websocket.dto.response.RockfallCardUsedResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RockfallCardUsedResponseTest {

    @Test
    void testUnicastResponse() {
        Long gameId = 1L;
        List<Integer> playerHand = List.of(101, 102, 103);
        Integer[][][] field = new Integer[7][9][2];

        RockfallCardUsedResponse response = new RockfallCardUsedResponse(gameId, playerHand, field);
        RockfallCardUsedResponse unicast = response.unicast();

        assertEquals(gameId, unicast.gameId());
        assertEquals(playerHand, unicast.playerHand());
        assertNull(unicast.filed(), "Unicast 응답에서 field는 null이어야 합니다.");
    }

    @Test
    void testBroadcastResponse() {
        Long gameId = 2L;
        List<Integer> playerHand = List.of(1,2,3);
        Integer[][][] field = new Integer[7][9][2];

        RockfallCardUsedResponse response = new RockfallCardUsedResponse(gameId, playerHand, field);
        RockfallCardUsedResponse broadcast = response.broadcast();

        assertEquals(gameId, broadcast.gameId());
        assertNull(broadcast.playerHand(), "Broadcast 응답에서 playerHand는 null이어야 합니다.");
        assertEquals(field, broadcast.filed());
    }
}