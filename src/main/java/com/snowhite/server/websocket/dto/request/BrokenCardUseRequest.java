package com.snowhite.server.websocket.dto.request;

public record BrokenCardUseRequest(
        Long gameId,
        Long playerId,
        Long targetPlayerId,
        Integer cardId
) {
}
