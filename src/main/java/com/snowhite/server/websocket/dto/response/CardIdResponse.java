package com.snowhite.server.websocket.dto.response;

public record CardIdResponse (
        int cardId
){
    public static CardIdResponse of(int cardId) {
        return new CardIdResponse(cardId);
    }
}
