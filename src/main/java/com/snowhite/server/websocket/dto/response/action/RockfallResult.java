package com.snowhite.server.websocket.dto.response.action;

import com.snowhite.server.websocket.dto.response.PublicPlayerResponse;
import com.snowhite.server.websocket.dto.response.SecretPlayerResponse;

import java.util.List;

public record RockfallResult(
        long playerId,
        int handSize,
        List<Integer> hand,
        Integer[][][] filed
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
                playerId,
                null,
                handSize,
                null,
                null
        );
    }

    public  GameChangedResult changedField() {
        return new GameChangedResult(filed);
    }

}
