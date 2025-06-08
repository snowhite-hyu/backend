package com.snowhite.server.websocket.dto;

import com.snowhite.server.domain.session.Room;

public class RoomServiceResultDTO {

    private final boolean success;
    private final String errorMessage;
    private final Room room;

    private RoomServiceResultDTO(boolean success, String errorMessage, Room room) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.room = room;
    }

    public static RoomServiceResultDTO success(Room room) {
        return new RoomServiceResultDTO(true, null, room);
    }

    public static RoomServiceResultDTO failure(String errorMessage) {
        return new RoomServiceResultDTO(false, errorMessage, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Room getRoom() {
        return room;
    }
}
