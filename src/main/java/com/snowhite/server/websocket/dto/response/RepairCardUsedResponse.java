package com.snowhite.server.websocket.dto.response;

import com.snowhite.server.domain.enums.PlayerState;

import java.util.List;

public record RepairCardUsedResponse(
        Long gameId,
        String message,
        List<Integer> playerHand,
        List<PlayerState> targetPlayerState
) {
    public RepairCardUsedResponse unicast() {
        return new RepairCardUsedResponse(
                gameId,
                message,
                playerHand,
                null
        );
    }

    public RepairCardUsedResponse broadcast() {
        return new RepairCardUsedResponse(
                gameId,
                message,
                null,
                targetPlayerState
        );
    }

}
