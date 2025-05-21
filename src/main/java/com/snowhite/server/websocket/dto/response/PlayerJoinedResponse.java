package com.snowhite.server.websocket.dto.response;

public record PlayerJoinedResponse(
        int remainingPlayers
) {
    public static PlayerJoinedResponse of(int remainingPlayers) {
        return new PlayerJoinedResponse(remainingPlayers);
    }
}
