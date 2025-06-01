package com.snowhite.server.websocket.dto.response;

import java.util.List;

public record MapCardUsedResponse(
        Long gameId,
        String message,
        List<Integer> playerHand,
        Integer cardId
) {
    public MapCardUsedResponse unicast() {
        return new MapCardUsedResponse(
                gameId,
                message,
                playerHand,
                cardId
        );
    }

    public MapCardUsedResponse broadcast() {
        return new MapCardUsedResponse(
                gameId,
                message,
                null,
                null
        );
    }
}
