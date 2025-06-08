package com.snowhite.server.websocket.dto;

import com.snowhite.server.domain.enums.PlayerState;
import com.snowhite.server.websocket.dto.response.PublicPlayerResponse;
import com.snowhite.server.websocket.dto.response.SecretPlayerResponse;
import com.snowhite.server.websocket.dto.response.TurnChangedResponse;

import java.util.EnumSet;
import java.util.List;

public record UseRepairCardDTO(
        TurnChangedResponse turnChangedResponse,
        SecretPlayerResponse secretPlayerResponse,
        PublicPlayerResponse publicPlayerResponse,
        PublicPlayerResponse publicTargetPlayerResponse

) {
    public static UseRepairCardDTO forNormalResult(
            TurnChangedResponse turnChangedResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse,
            PublicPlayerResponse publicTargetPlayerResponse
    ) {
        return new UseRepairCardDTO(
                turnChangedResponse,
                secretPlayerResponse,
                publicPlayerResponse,
                publicTargetPlayerResponse
        );
    }
}
