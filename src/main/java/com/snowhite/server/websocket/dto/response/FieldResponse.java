package com.snowhite.server.websocket.dto.response;

public record FieldResponse(
        int cardId,
        int row,
        int column,
        int isFlipped
) {
    public static FieldResponse of(int cardId, int row, int column, int isFlipped) {
        return new FieldResponse(cardId, row, column, isFlipped);
    }
}
