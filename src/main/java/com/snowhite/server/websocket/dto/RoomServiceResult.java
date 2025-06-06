package com.snowhite.server.websocket.dto;

import com.snowhite.server.domain.session.Room;

public class RoomServiceResult {

    private final boolean success;
    private final String errorMessage;
    private final Room room;

    private RoomServiceResult(boolean success, String errorMessage, Room room) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.room = room;
    }

    public static RoomServiceResult success(Room room) {
        return new RoomServiceResult(true, null, room);
    }

    public static RoomServiceResult failure(String errorMessage) {
        return new RoomServiceResult(false, errorMessage, null);
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
