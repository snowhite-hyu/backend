package com.snowhite.server.websocket.dto;

import com.snowhite.server.websocket.dto.response.*;


public record UseMapCardResultDTO(
        TurnChangedResponse turnChangedResponse,
        RoundFinishedResponse roundFinishedResponse,
        SecretPlayerResponse secretPlayerResponse,
        PublicPlayerResponse publicPlayerResponse,
        CardIdResponse cardIdResponse,
        boolean isRoundFinished
) {
    public static UseMapCardResultDTO forNormalResult(
            TurnChangedResponse turnChangedResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse,
            CardIdResponse cardIdResponse
    ) {
        return new UseMapCardResultDTO(
                turnChangedResponse,
                null,
                secretPlayerResponse,
                publicPlayerResponse,
                cardIdResponse,
                false
        );
    }
    public static UseMapCardResultDTO forRoundFinishedResult(
            RoundFinishedResponse roundFinishedResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse,
            CardIdResponse cardIdResponse,
            boolean isRoundFinished
    ) {
        return new UseMapCardResultDTO(
                null,
                roundFinishedResponse,
                secretPlayerResponse,
                publicPlayerResponse,
                cardIdResponse,
                isRoundFinished
        );
    }
}
