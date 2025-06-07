package com.snowhite.server.websocket.dto;

import com.snowhite.server.websocket.dto.response.PublicPlayerResponse;
import com.snowhite.server.websocket.dto.response.SecretPlayerResponse;
import com.snowhite.server.websocket.dto.response.TurnChangedResponse;

import java.util.List;

public record UseMapCardResultDTO(
        TurnChangedResponse turnChangedResponse,
        SecretPlayerResponse secretPlayerResponse,
        PublicPlayerResponse publicPlayerResponse
) {
    public static UseMapCardResultDTO forNormalResult(
            TurnChangedResponse turnChangedResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse
    ) {
        return new UseMapCardResultDTO(
                turnChangedResponse,
                secretPlayerResponse,
                publicPlayerResponse
        );
    }
}
