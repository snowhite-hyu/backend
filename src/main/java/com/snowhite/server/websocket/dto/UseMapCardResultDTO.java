package com.snowhite.server.websocket.dto;

import com.snowhite.server.websocket.dto.response.*;

import java.util.List;

public record UseMapCardResultDTO(
        TurnChangedResponse turnChangedResponse,
        RoundFinishedResponse roundFinishedResponse,
        SecretPlayerResponse secretPlayerResponse,
        PublicPlayerResponse publicPlayerResponse,
        int destCardId,
        boolean isRoundFinished
) {
    public static UseMapCardResultDTO forNormalResult(
            TurnChangedResponse turnChangedResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse,
            Integer destCardId
    ) {
        return new UseMapCardResultDTO(
                turnChangedResponse,
                null,
                secretPlayerResponse,
                publicPlayerResponse,
                destCardId,
                false
        );
    }
    public static UseMapCardResultDTO forRoundFinishedResult(
            RoundFinishedResponse roundFinishedResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse,
            Integer destCardId,
            boolean isRoundFinished
    ) {
        return new UseMapCardResultDTO(
                null,
                roundFinishedResponse,
                secretPlayerResponse,
                publicPlayerResponse,
                destCardId,
                isRoundFinished
        );
    }
}
