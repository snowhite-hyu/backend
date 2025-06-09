package com.snowhite.server.websocket.dto;

import com.snowhite.server.websocket.dto.response.PublicPlayerResponse;
import com.snowhite.server.websocket.dto.response.RoundFinishedResponse;
import com.snowhite.server.websocket.dto.response.SecretPlayerResponse;
import com.snowhite.server.websocket.dto.response.TurnChangedResponse;

public record UseBrokenCardResultDTO(
        TurnChangedResponse turnChangedResponse,
        RoundFinishedResponse roundFinishedResponse,
        SecretPlayerResponse secretPlayerResponse,
        PublicPlayerResponse publicPlayerResponse,
        PublicPlayerResponse publicTargetPlayerResponse

) {
    public static UseRepairCardResultDTO forNormalResult(
            TurnChangedResponse turnChangedResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse,
            PublicPlayerResponse publicTargetPlayerResponse
    ) {
        return new UseRepairCardResultDTO(
                turnChangedResponse,
                null,
                secretPlayerResponse,
                publicPlayerResponse,
                publicTargetPlayerResponse
        );
    }

    public static UseBrokenCardResultDTO forRoundFinishedResult(
            RoundFinishedResponse roundFinishedResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse,
            PublicPlayerResponse publicTargetPlayerResponse
    ) {
        return new UseBrokenCardResultDTO(
                null,
                roundFinishedResponse,
                secretPlayerResponse,
                publicPlayerResponse,
                publicTargetPlayerResponse
        );
    }
}
