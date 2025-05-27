package com.snowhite.server.web.controller;

import com.snowhite.server.payload.ApiResponse;
import com.snowhite.server.service.RoomService;
import com.snowhite.server.web.dto.response.StartGameResponse;
import com.snowhite.server.web.dto.response.GetRoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping("/{roomId}/start")
    public Mono<ApiResponse<StartGameResponse>> startGame(@PathVariable Long roomId) {

        Mono<StartGameResponse> startGameResponse = roomService.startGameByRoomId(roomId);

        Mono<ApiResponse<StartGameResponse>> result = startGameResponse.map(ApiResponse::onSuccess);
        return result;
    }

    @GetMapping()
    public Mono<ApiResponse<GetRoomResponse>> getRooms() {

        Mono<GetRoomResponse> getRoomResponse = roomService.getRooms();

        Mono<ApiResponse<GetRoomResponse>> result = getRoomResponse.map(ApiResponse::onSuccess);
        return result;

    }
}
