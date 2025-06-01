package com.snowhite.server.websocket.dto.request;

public record MapCardUseRequest(
        Long gameId,
        Long playerId,
        Integer cardId,
        Integer row,
        Integer column
) {
}
