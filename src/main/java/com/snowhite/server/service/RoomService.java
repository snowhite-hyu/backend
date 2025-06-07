package com.snowhite.server.service;

import com.snowhite.server.domain.session.Game;
import com.snowhite.server.domain.session.Player;
import com.snowhite.server.domain.session.Room;
import com.snowhite.server.web.dto.response.StartGameResponse;
import com.snowhite.server.web.dto.response.GetRoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private static final String ROOM_PREFIX = "room:";

    private final ReactiveRedisTemplate<String, Room> reactiveRedisTemplateForRoom;

    private final GameService gameService;

    public Mono<StartGameResponse> startGameByRoomId(Long roomId) {

        Mono<Room> roomForStart = getRoomByRoomId(roomId);

        return roomForStart
                .flatMap(room -> {
                    List<Player> players = room.getUsers().stream()
                            .map(user -> new Player(user.getId(), user.getUsername()))
                            .toList();
                    int turnTime = room.getTurnTime();
                    Game newGame = new Game(roomId, players, turnTime);
                    return gameService.setGameToRedis(roomId, newGame)
                            .then(deleteRoomByRoomId(roomId))
                            .thenReturn(StartGameResponse.of(roomId));
                });
    }

    public Mono<GetRoomResponse> getRooms() {

        return scanRoomKeys()
                .flatMap(roomId -> reactiveRedisTemplateForRoom.opsForValue().get(roomId))
                .collectList()
                .map(rooms -> {
                    if (rooms == null) {
                        rooms = Collections.emptyList();
                    }
                    return GetRoomResponse.of(rooms);
                });
    }

    public Flux<String> scanRoomKeys() {
        ScanOptions options =
                ScanOptions.scanOptions().match("room:*").count(1000).build();
        return reactiveRedisTemplateForRoom.scan(options);
    }

    public Mono<Room> getRoomByRoomId(Long roomId) {
        return reactiveRedisTemplateForRoom.opsForValue().get(ROOM_PREFIX + roomId);
    }

    public Mono<Boolean> deleteRoomByRoomId(Long roomId) {
        return reactiveRedisTemplateForRoom.opsForValue().delete(ROOM_PREFIX + roomId);
    }

}
