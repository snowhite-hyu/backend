package com.snowhite.server.websocket.dto.response;

public record TurnChangedResponse(
        Long nextTurnPlayerId
) {
    public static TurnChangedResponse of(Long nextTurnPlayerId) {
        return new TurnChangedResponse(nextTurnPlayerId);
    }
}
