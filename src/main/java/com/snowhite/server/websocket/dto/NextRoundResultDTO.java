package com.snowhite.server.websocket.dto;

import com.snowhite.server.websocket.dto.response.GameResponse;
import com.snowhite.server.websocket.dto.response.PublicPlayerResponse;

public record NextRoundResultDTO(
        boolean isGameFinished,
        GameResponse gameResponse,
        PublicPlayerResponse winnerPublicPlayerResponse
) {
    public static NextRoundResultDTO forRoundStart(GameResponse gameResponse) {
        return new NextRoundResultDTO(false, gameResponse, null);
    }

    public static NextRoundResultDTO forFinishGame(PublicPlayerResponse publicPlayerResponse) {
        return new NextRoundResultDTO(true, null, publicPlayerResponse);
    }
}
