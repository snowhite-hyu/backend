package com.snowhite.server.service;

import com.snowhite.server.domain.session.Game;
import com.snowhite.server.domain.session.Player;
import com.snowhite.server.domain.session.Room;
import com.snowhite.server.payload.ApiResponse;
import com.snowhite.server.web.dto.web.response.GetRoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private static final String ROOM_PREFIX = "room:";
    private static final String GAME_PREFIX = "game:";

    private final ReactiveRedisTemplate<String, Game> reactiveRedisTemplateForGame;
    private final ReactiveRedisTemplate<String, Room> reactiveRedisTemplateForRoom;

    public Mono<Long> startGameByRoomId(Long roomId) {

        Mono<Room> roomForStart = reactiveRedisTemplateForRoom.opsForValue().get(ROOM_PREFIX + roomId);

        return roomForStart
                .flatMap(room -> {
                    List<Player> players = room.getUsers().stream()
                            .map(user -> new Player(user.getId(), user.getUsername()))
                            .toList();

                    Game newGame = new Game(roomId, players);

                    return reactiveRedisTemplateForGame.opsForValue().set(GAME_PREFIX + roomId, newGame)
                            .thenReturn(roomId);
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

}
