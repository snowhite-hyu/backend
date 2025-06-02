package com.snowhite.server.websocket.dto.request;

public record MapCardUseRequest(
        Long gameId,
        Long playerId,
        int cardId,
        Integer row,
        Integer column
) {
}
