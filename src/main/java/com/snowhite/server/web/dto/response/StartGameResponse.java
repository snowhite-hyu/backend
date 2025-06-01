package com.snowhite.server.web.dto.response;

public record StartGameResponse(
        long gameId
) {
    public static StartGameResponse of(long gameId) {
        return new StartGameResponse(gameId);
    }
}
