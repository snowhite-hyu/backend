package com.snowhite.server.websocket.dto;

import com.snowhite.server.websocket.dto.response.*;

import java.util.List;

public record UsePathCardResultDTO(
        boolean isPossibleToPlace,
        boolean isRoundFinished,
        boolean isGameFinished,
        TurnChangedResponse turnChangedResponse,
        FieldResponse fieldResponse,
        SecretPlayerResponse secretPlayerResponse,
        PublicPlayerResponse publicPlayerResponse,
        RoundFinishedResponse roundFinishedResponse
) {

    public static UsePathCardResultDTO forPlacePathCardFailedResult(FieldResponse fieldResponse) {
        return new UsePathCardResultDTO(
                false,
                false,
                false,
                null,
                fieldResponse,
                null,
                null,
                null);
    }

    public static UsePathCardResultDTO forRoundFinishedResult(
            FieldResponse fieldResponse,
            PublicPlayerResponse publicPlayerResponse,
            RoundFinishedResponse roundFinishedResponse) {

        return new UsePathCardResultDTO(
                true,
                true,
                false,
                null,
                fieldResponse,
                null,
                publicPlayerResponse,
                roundFinishedResponse
        );
    }

    public static UsePathCardResultDTO forNormalResult(
            TurnChangedResponse turnChangedResponse,
            FieldResponse fieldResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse
    ) {
        return new UsePathCardResultDTO(
                true,
                false,
                false,
                turnChangedResponse,
                fieldResponse,
                secretPlayerResponse,
                publicPlayerResponse,
                null
        );
    }

}
