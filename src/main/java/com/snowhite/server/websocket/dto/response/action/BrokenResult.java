package com.snowhite.server.websocket.dto.response.action;

import com.snowhite.server.domain.enums.PlayerState;
import com.snowhite.server.websocket.dto.response.PublicPlayerResponse;
import com.snowhite.server.websocket.dto.response.SecretPlayerResponse;

import java.util.EnumSet;
import java.util.List;

public record BrokenResult(
    long playerId,
    long targetPlayerId,
    int handSize,
    List<Integer> hand,
    EnumSet<PlayerState> targetPlayerState
) {
    public SecretPlayerResponse getSecretInfo() {
        return new SecretPlayerResponse(
                playerId,
                null,
                null,
                hand,
                null,
                null
        );
    }

    public PublicPlayerResponse getPublicInfo() {
        return new PublicPlayerResponse(
                targetPlayerId,
                null,
                handSize,
                targetPlayerState,
                null
        );
    }
}
