package com.snowhite.server.web.dto.response;

import com.snowhite.server.domain.session.Room;

import java.util.List;

public record GetRoomResponse(
        List<Room> roomList
) {
    public static GetRoomResponse of(List<Room> roomList) {
        return new GetRoomResponse(roomList);
    }
}
