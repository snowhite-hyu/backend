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
public class RoomService {

    private static final String ROOM_PREFIX = "room:";
    private static final String ROOM_SESSION_PREFIX = "room_session:";

    private static final AtomicLong roomIdGenerator = new AtomicLong(0);

    private final UserRepository userRepository;
    private final ReactiveRedisTemplate<String, Game> reactiveRedisTemplateForGame;
    private final ReactiveRedisTemplate<String, Room> reactiveRedisTemplateForRooms;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplateForSessionIds;

    private final GameService gameService;

    public RoomService (
            UserRepository userRepository,
            ReactiveRedisTemplate<String, Game> reactiveRedisTemplateForGame,
            @Qualifier("reactiveRedisTemplateForRooms")
            ReactiveRedisTemplate<String, Room> reactiveRedisTemplateForRooms,
            @Qualifier("reactiveRedisTemplateForSessionIds")
            ReactiveRedisTemplate<String, String> reactiveRedisTemplateForSessionIds,
            GameService gameService
    ) {
        this.userRepository = userRepository;
        this.reactiveRedisTemplateForGame = reactiveRedisTemplateForGame;
        this.reactiveRedisTemplateForRooms = reactiveRedisTemplateForRooms;
        this.reactiveRedisTemplateForSessionIds = reactiveRedisTemplateForSessionIds;
        this.gameService = gameService;
    }

    public Mono<RoomServiceResult> createRoom(long userId, String roomName, int capacity, int turnTime) {

        return Mono.fromCallable(() -> userRepository.findById(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optUser -> {
                    if (optUser.isEmpty()) {
                        return Mono.just(RoomServiceResult.failure("User is not found"));
                    }
                    User user = optUser.get();
                    Long roomId = roomIdGenerator.incrementAndGet();

                    Room room = new Room(roomId, roomName, user, new ArrayList<>(List.of(user)), capacity, turnTime, false);

                    return reactiveRedisTemplateForRooms.opsForValue()
                            .set(ROOM_PREFIX + roomId, room)
                            .thenReturn(RoomServiceResult.success(room));
                });
    }

    public Mono<RoomServiceResult> joinRoom(long userId, Long roomId) {

        return Mono.fromCallable(() -> userRepository.findById(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optUser -> {
                    if (optUser.isEmpty()) {
                        return Mono.just(RoomServiceResult.failure("User is not found"));
                    }

                    User user = optUser.get();
                    return reactiveRedisTemplateForRooms.opsForValue().get(ROOM_PREFIX + roomId)
                            .flatMap(room -> {

                                if (room.getUsers().stream().anyMatch(u -> u.getId() == userId)) {
                                    return Mono.just(RoomServiceResult.failure("User is already in room"));
                                }

                                room.getUsers().add(user);

                                return reactiveRedisTemplateForRooms.opsForValue()
                                        .set(ROOM_PREFIX + roomId, room)
                                        .thenReturn(RoomServiceResult.success(room));
                            })
                            .switchIfEmpty(Mono.just(RoomServiceResult.failure("Room is not found")));
                });
    }

    public Mono<RoomServiceResult> quitRoom(long userId, Long roomId, String sessionId) {

        return Mono.fromCallable(() -> userRepository.findById(userId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optUser -> {
                    if (optUser.isEmpty()) {
                        return Mono.just(RoomServiceResult.failure("User is not found"));
                    }

                    User user = optUser.get();

                    return reactiveRedisTemplateForRooms.opsForValue().get(ROOM_PREFIX + roomId)
                            .flatMap(room -> {

                                if (room.getUsers().stream().noneMatch(u -> u.getId() == userId)) {
                                    return Mono.just(RoomServiceResult.failure("User is not in room"));
                                }

                                boolean isMaster = userId == room.getMasterPlayer().getId();

                                List<User> users = room.getUsers();

                                if (isMaster && users.size() > 1) {
                                    return Mono.just(RoomServiceResult.failure(
                                            "Master player can not quit room while other users remain in room"
                                    ));
                                }

                                List<User> updatedUsers = room.getUsers()
                                        .stream()
                                        .filter(u -> u.getId() != userId)
                                        .collect(Collectors.toList());

                                room.setUsers(updatedUsers);

                                Mono<Boolean> updateRoomMono = (isMaster && updatedUsers.isEmpty())
                                        ? reactiveRedisTemplateForRooms.delete(ROOM_PREFIX + roomId).thenReturn(true)
                                        : reactiveRedisTemplateForRooms.opsForValue().set(ROOM_PREFIX + roomId, room);

                                return updateRoomMono
                                        .then(reactiveRedisTemplateForSessionIds.delete(ROOM_SESSION_PREFIX + String.valueOf(userId)))
                                        .thenReturn(RoomServiceResult.success(room));
                            })
                            .switchIfEmpty(Mono.just(RoomServiceResult.failure("Room is not found")));
                });
    }

    public Mono<GetRoomResponse> getRooms() {

        return scanRoomKeys()
                .flatMap(roomId -> reactiveRedisTemplateForRooms.opsForValue().get(roomId))
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
                ScanOptions.scanOptions().match(ROOM_PREFIX + "*").count(1000).build();
        return reactiveRedisTemplateForRooms.scan(options);
    }

    public Mono<Room> getRoomByRoomId(Long roomId) {
        return reactiveRedisTemplateForRooms.opsForValue().get(ROOM_PREFIX + roomId);
    }

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
                            .thenReturn(StartGameResponse.of(roomId));
                });
    }

}
