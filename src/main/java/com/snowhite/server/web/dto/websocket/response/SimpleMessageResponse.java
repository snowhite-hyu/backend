package com.snowhite.server.web.dto.websocket.response;

public record SimpleMessageResponse(
        String message
) {
    public static SimpleMessageResponse of(String message) {
        return new SimpleMessageResponse(message);
    }
}
