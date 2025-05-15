package com.snowhite.server.web.controller;

import com.snowhite.server.payload.ApiResponse;
import com.snowhite.server.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping("/{roomId}/start")
    public ApiResponse<Void> startGame(@PathVariable Long roomId) {

        roomService.startGameByRoomId(roomId);
        // TODO: socket 연결 및 response

        return ApiResponse.onSuccess();
    }
}
