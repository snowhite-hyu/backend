package com.snowhite.server.websocket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.snowhite.server.domain.enums.PlayerState;

import java.util.List;
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BrokenCardUsedResponse(
        Long gameId,
        Long playerId,
        Long targetPlayerId,
        Integer cardId,
        List<Integer> playerHand,
        List<PlayerState> targetPlayerState
) {
    public BrokenCardUsedResponse unicast() {
        return new BrokenCardUsedResponse(
                gameId,
                playerId,
                null,
                null,
                playerHand,
                null
        );
    }
    public BrokenCardUsedResponse broadcast() {
        return new BrokenCardUsedResponse(
                gameId,
                playerId,
                targetPlayerId,
                cardId,
                null,
                targetPlayerState
        );
    }
}
