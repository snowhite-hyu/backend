package com.snowhite.server.websocket.dto.request;

public record RockfallCardUseRequest(
        Long gameId,
        Long playerId,
        int cardId,
        int row,
        int column
) {
}