package com.snowhite.server.websocket.dto.response;

public record FieldResponse(
        int cardId,
        int row,
        int column,
        int isRotated,
        int isFlipped
) {
    public static FieldResponse of(int cardId, int row, int column, int isRotated, int isFlipped) {
        return new FieldResponse(cardId, row, column, isRotated, isFlipped);
    }
}
