package com.snowhite.server.websocket.dto;

import com.snowhite.server.websocket.dto.response.*;

import java.util.List;

public record UseMapCardResultDTO(
        TurnChangedResponse turnChangedResponse,
        RoundFinishedResponse roundFinishedResponse,
        SecretPlayerResponse secretPlayerResponse,
        PublicPlayerResponse publicPlayerResponse,
        Integer cardId
) {
    public static UseMapCardResultDTO forNormalResult(
            TurnChangedResponse turnChangedResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse,
            int cardId
    ) {
        return new UseMapCardResultDTO(
                turnChangedResponse,
                null,
                secretPlayerResponse,
                publicPlayerResponse,
                cardId
        );
    }
    public static UseMapCardResultDTO forRoundFinishedResult(
            RoundFinishedResponse roundFinishedResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse
    ) {
        return new UseMapCardResultDTO(
                null,
                roundFinishedResponse,
                secretPlayerResponse,
                publicPlayerResponse,
                null
        );
    }
}
