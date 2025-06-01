package com.snowhite.server.websocket.dto.response.nextround;

import com.snowhite.server.websocket.dto.response.GameResponse;

public record NextRoundGameResponse(
        GameResponse game
) implements NextRoundResponse {
    public static NextRoundGameResponse of(GameResponse game) {
        return new NextRoundGameResponse(game);
    }
}
