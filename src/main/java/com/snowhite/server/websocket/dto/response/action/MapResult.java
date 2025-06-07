package com.snowhite.server.websocket.dto.response.action;

import com.snowhite.server.websocket.dto.response.SecretPlayerResponse;

import java.util.List;

public record MapResult(
        long playerId,
        int handSize,
        List<Integer> hand
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
}
