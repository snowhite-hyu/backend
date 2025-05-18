package com.snowhite.server.websocket.dto.response;

public record SimpleMessageResponse(
        String message
) {
    public static SimpleMessageResponse of(String message) {
        return new SimpleMessageResponse(message);
    }
}
