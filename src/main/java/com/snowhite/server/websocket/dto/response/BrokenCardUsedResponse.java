package com.snowhite.server.websocket.dto.response;

import com.snowhite.server.domain.enums.PlayerState;

import java.util.List;

public record BrokenCardUsedResponse(
        Long gameId,
        List<Integer> playerHand,
        List<PlayerState> targetPlayerState
) {
    public BrokenCardUsedResponse unicast() {
        return new BrokenCardUsedResponse(
                gameId,
                playerHand,
                null
        );
    }

    public BrokenCardUsedResponse broadcast() {
        return new BrokenCardUsedResponse(
                gameId,
                null,
                targetPlayerState
        );
    }
}
