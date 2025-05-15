package com.snowhite.server.service;

import com.snowhite.server.domain.session.Game;
import com.snowhite.server.domain.session.Player;
import com.snowhite.server.domain.session.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private static final String ROOM_PREFIX = "room: ";
    private static final String GAME_PREFIX = "game: ";

    private final ReactiveRedisTemplate<String, Game> reactiveRedisTemplateForGame;

    private final ReactiveRedisTemplate<String, Room> reactiveRedisTemplateForRoom;

    public Long startGameByRoomId(Long roomId) {

        Mono<Room> roomForStart = reactiveRedisTemplateForRoom.opsForValue().get(ROOM_PREFIX + roomId);

        // Game 객체 생성 후 Redis 저장
        Mono<Boolean> result = roomForStart
                .flatMap(room -> {
                    List<Player> players = room.getUsers().stream()
                            .map(user -> new Player(user.getId(), user.getUsername()))
                            .toList();

                    Game newGame = new Game(roomId, players);

                    return reactiveRedisTemplateForGame.opsForValue().set(GAME_PREFIX + roomId, newGame);
                });

        return roomId;
    }

}
