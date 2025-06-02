package com.snowhite.server.websocket.dto.response;

import java.util.List;

public record RockfallCardUsedResponse(
        Long gameId,
        List<Integer> playerHand,
        Integer[][][] filed
) {

    public RockfallCardUsedResponse unicast() {
        return new RockfallCardUsedResponse(
                gameId,
                playerHand,
                null
        );
    }

    public RockfallCardUsedResponse broadcast() {
        return new RockfallCardUsedResponse(
                gameId,
                null,
                filed
        );
    }
}
