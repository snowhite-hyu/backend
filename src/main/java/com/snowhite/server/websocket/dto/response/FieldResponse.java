package com.snowhite.server.websocket.dto.response;

public record FieldResponse(
        int cardId,
        int row,
        int column,
        int isSpun
) {
    public static FieldResponse of(int cardId, int row, int column, int isSpun) {
        return new FieldResponse(cardId, row, column, isSpun);
    }
}
