package com.snowhite.server.websocket.dto;

import com.snowhite.server.websocket.dto.response.FieldResponse;
import com.snowhite.server.websocket.dto.response.PublicPlayerResponse;
import com.snowhite.server.websocket.dto.response.RoundFinishedResponse;
import com.snowhite.server.websocket.dto.response.SecretPlayerResponse;

import java.util.List;

public record UsePathCardResultDTO(
        boolean isPossibleToPlace,
        boolean isRoundFinished,
        boolean isGameFinished,
        FieldResponse fieldResponse,
        PublicPlayerResponse publicPlayerResponse,
        RoundFinishedResponse roundFinishedResponse
) {

    public static UsePathCardResultDTO forPlacePathCardFailedResult(FieldResponse fieldResponse) {
        return new UsePathCardResultDTO(
                false,
                false,
                false,
                fieldResponse,
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
                fieldResponse,
                publicPlayerResponse,
                roundFinishedResponse
        );
    }

    public static UsePathCardResultDTO forNormalResult(
            FieldResponse fieldResponse,
            PublicPlayerResponse publicPlayerResponse
    ) {
        return new UsePathCardResultDTO(
                true,
                false,
                false,
                fieldResponse,
                publicPlayerResponse,
                null
        );
    }

}
