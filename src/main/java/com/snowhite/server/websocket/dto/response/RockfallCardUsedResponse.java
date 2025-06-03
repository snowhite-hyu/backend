package com.snowhite.server.websocket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RockfallCardUsedResponse(
        Long gameId,
        Long playerId,
        Integer cardId,
        Integer row,
        Integer column,
        List<Integer> playerHand,
        Integer[][][] filed
) {

    public RockfallCardUsedResponse unicast() {
        return new RockfallCardUsedResponse(
                gameId,
                playerId
                ,null,
                null,
                null,
                playerHand,
                null
        );
    }

    public RockfallCardUsedResponse broadcast() {
        return new RockfallCardUsedResponse(
                gameId,
                playerId,
                cardId,
                row,
                column,
                null,
                filed
        );
    }
}
