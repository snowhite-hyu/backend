package com.snowhite.server.websocket.dto;

import com.snowhite.server.websocket.dto.response.*;

import java.util.List;

public record UsePathCardResultDTO(
        boolean isPossibleToPlace,
        boolean isRoundFinished,
        boolean shouldRevealDestination,
        TurnChangedResponse turnChangedResponse,
        FieldResponse fieldResponse,
        SecretPlayerResponse secretPlayerResponse,
        PublicPlayerResponse publicPlayerResponse,
        RoundFinishedResponse roundFinishedResponse,
        List<FieldResponse> destinationResponseList

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
                null,
                null
        );
    }

    public static UsePathCardResultDTO forRoundFinishedResult(
            boolean shouldRevealDestination,
            FieldResponse fieldResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse,
            RoundFinishedResponse roundFinishedResponse,
            List<FieldResponse> destinationResponseList) {

        return new UsePathCardResultDTO(
                shouldRevealDestination,
                true,
                false,
                null,
                fieldResponse,
                secretPlayerResponse,
                publicPlayerResponse,
                roundFinishedResponse,
                destinationResponseList
        );
    }

    public static UsePathCardResultDTO forNormalResult(
            boolean shouldRevealDestination,
            TurnChangedResponse turnChangedResponse,
            FieldResponse fieldResponse,
            SecretPlayerResponse secretPlayerResponse,
            PublicPlayerResponse publicPlayerResponse,
            List<FieldResponse> destinationResponseList
    ) {
        return new UsePathCardResultDTO(
                true,
                false,
                false,
                turnChangedResponse,
                fieldResponse,
                secretPlayerResponse,
                publicPlayerResponse,
                null,
                destinationResponseList
        );
    }

}
