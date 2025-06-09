package com.snowhite.server.websocket.dto;

import com.snowhite.server.websocket.dto.response.*;

public record UseRockfallCardResultDTO(
    TurnChangedResponse turnChangedResponse,
    RoundFinishedResponse roundFinishedResponse,
    SecretPlayerResponse secretPlayerResponse,
    PublicPlayerResponse publicPlayerResponse,
    FieldResponse fieldResponse,
    boolean isRoundFinished
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
                fieldResponse,
                false
        );
    }

    public static UseRockfallCardResultDTO forRoundFinishedResult(
            RoundFinishedResponse roundFinishedResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse,
            FieldResponse fieldResponse,
            boolean isRoundFinished
    ) {
        return new UseRockfallCardResultDTO(
                null,
                roundFinishedResponse,
                secretPlayerResponse,
                publicPlayerResponse,
                fieldResponse,
                isRoundFinished
        );
    }
}
