package com.snowhite.server.websocket.dto;

import com.snowhite.server.websocket.dto.response.FieldResponse;
import com.snowhite.server.websocket.dto.response.PublicPlayerResponse;
import com.snowhite.server.websocket.dto.response.SecretPlayerResponse;
import com.snowhite.server.websocket.dto.response.TurnChangedResponse;

public record UseRockfallCardResultDTO(
    TurnChangedResponse turnChangedResponse,
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
                secretPlayerResponse,
                publicPlayerResponse,
                fieldResponse
        );
    }

}
