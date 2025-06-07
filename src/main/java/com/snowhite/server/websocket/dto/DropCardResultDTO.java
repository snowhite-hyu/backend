package com.snowhite.server.websocket.dto;

import com.snowhite.server.websocket.dto.response.PublicPlayerResponse;
import com.snowhite.server.websocket.dto.response.RoundFinishedResponse;
import com.snowhite.server.websocket.dto.response.SecretPlayerResponse;
import com.snowhite.server.websocket.dto.response.TurnChangedResponse;

public record DropCardResultDTO(
        boolean isRoundFinished,
        SecretPlayerResponse secretPlayerResponse,
        PublicPlayerResponse publicPlayerResponse,
        TurnChangedResponse turnChangedResponse,
        RoundFinishedResponse roundFinishedResponse
) {

    public static DropCardResultDTO forRoundFinished(
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse,
            RoundFinishedResponse roundFinishedResponse
    ) {
        return new DropCardResultDTO(
                true,
                secretPlayerResponse,
                publicPlayerResponse,
                null,
                roundFinishedResponse
        );
    }

    public static DropCardResultDTO forNextTurn(
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse,
            TurnChangedResponse turnChangedResponse
    ) {
        return new DropCardResultDTO(
                false,
                secretPlayerResponse,
                publicPlayerResponse,
                turnChangedResponse,
                null
        );
    }
}
