package com.snowhite.server.websocket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.snowhite.server.domain.enums.PlayerState;

import java.util.EnumSet;
import java.util.List;
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RepairCardUsedResponse(
        Long gameId,
        Long playerId,
        Long targetPlayerId,
        Integer cardId,
        List<Integer> playerHand,
        EnumSet<PlayerState> targetPlayerState
) {
    public RepairCardUsedResponse unicast() {
        return new RepairCardUsedResponse(
                gameId,
                playerId,
                null,
                null,
                playerHand,
                null
        );
    }

    public RepairCardUsedResponse broadcast() {
        return new RepairCardUsedResponse(
                gameId,
                playerId,
                targetPlayerId,
                cardId,
                null,
                targetPlayerState
        );
    }

}
