package com.snowhite.server.websocket.dto.response;

public record FieldResponse(
        int cardId,
        int row,
        int column,
        int isRotated
) {
    public static FieldResponse of(int cardId, int row, int column, int isRotated) {
        return new FieldResponse(cardId, row, column, isRotated);
    }
}
