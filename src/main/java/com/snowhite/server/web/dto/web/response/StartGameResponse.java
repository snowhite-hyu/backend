package com.snowhite.server.web.dto.web.response;

public record StartGameResponse(
        long gameId
) {
    public static StartGameResponse of(long gameId) {
        return new StartGameResponse(gameId);
    }
}
