package com.snowhite.server.websocket.dto.response;

import com.snowhite.server.domain.enums.PlayerState;

import java.util.EnumSet;
import java.util.List;

public record RepairCardUsedResponse(
        Long gameId,
        List<Integer> playerHand,
        EnumSet<PlayerState> targetPlayerState
) {
    public RepairCardUsedResponse unicast() {
        return new RepairCardUsedResponse(
                gameId,
                playerHand,
                null
        );
    }

    public RepairCardUsedResponse broadcast() {
        return new RepairCardUsedResponse(
                gameId,
                null,
                targetPlayerState
        );
    }

}
