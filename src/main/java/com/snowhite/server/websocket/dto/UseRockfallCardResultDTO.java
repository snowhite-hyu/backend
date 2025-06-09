package com.snowhite.server.websocket.dto;

import com.snowhite.server.websocket.dto.response.*;

public record UseRockfallCardResultDTO(
    TurnChangedResponse turnChangedResponse,
    RoundFinishedResponse roundFinishedResponse,
    SecretPlayerResponse secretPlayerResponse,
    PublicPlayerResponse publicPlayerResponse,
    FieldResponse fieldResponse
) {
    public static UseRockfallCardResultDTO forNormalResult(
            TurnChangedResponse turnChangedResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse,
            FieldResponse fieldResponse
    ) {
        return new UseRockfallCardResultDTO(
                turnChangedResponse,
                null,
                secretPlayerResponse,
                publicPlayerResponse,
                fieldResponse
        );
    }

    public static UseRockfallCardResultDTO forRoundFinishedResult(
            RoundFinishedResponse roundFinishedResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse,
            FieldResponse fieldResponse
    ) {
        return new UseRockfallCardResultDTO(
                null,
                roundFinishedResponse,
                secretPlayerResponse,
                publicPlayerResponse,
                fieldResponse
        );
    }
}
