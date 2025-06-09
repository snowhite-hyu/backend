package com.snowhite.server.websocket.dto;

import com.snowhite.server.websocket.dto.response.GameResponse;
import com.snowhite.server.websocket.dto.response.PublicPlayerResponse;
import com.snowhite.server.websocket.dto.response.SecretPlayerResponse;

import java.util.List;

public record NextRoundResultDTO(
        boolean isGameFinished,
        GameResponse gameResponse,
        List<SecretPlayerResponse> secretPlayerResponseList,
        PublicPlayerResponse winnerPublicPlayerResponse
) {
    public static NextRoundResultDTO forRoundStart(GameResponse gameResponse, List<SecretPlayerResponse> secretPlayerResponseList) {
        return new NextRoundResultDTO(false, gameResponse, secretPlayerResponseList,null);
    }

    public static NextRoundResultDTO forFinishGame(PublicPlayerResponse publicPlayerResponse) {
        return new NextRoundResultDTO(true, null, null, publicPlayerResponse);
    }
}
