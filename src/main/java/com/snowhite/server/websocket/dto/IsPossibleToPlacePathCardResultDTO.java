package com.snowhite.server.websocket.dto;

public record IsPossibleToPlacePathCardResultDTO(
        boolean isPossible,
        boolean isMiddleOpened
) {
    public static IsPossibleToPlacePathCardResultDTO of(boolean isPossible, boolean isMiddleOpened) {
        return new IsPossibleToPlacePathCardResultDTO(isPossible, isMiddleOpened);
    }
}
