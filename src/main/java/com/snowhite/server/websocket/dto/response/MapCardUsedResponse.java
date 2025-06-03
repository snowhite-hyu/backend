package com.snowhite.server.websocket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MapCardUsedResponse(
        Long gameId,
        Long playerId,
        Integer cardId,
        Integer row,
        Integer column,
        List<Integer> playerHand,
        Integer destCardId
) {
    public MapCardUsedResponse unicast() {
        return new MapCardUsedResponse(
                gameId,
                playerId,
                null,
                null,
                null,
                playerHand,
                destCardId
        );
    }

    public MapCardUsedResponse broadcast() {
        return new MapCardUsedResponse(
                gameId,
                playerId,
                cardId,
                row,
                column,
                null,
                null
        );
    }
}
