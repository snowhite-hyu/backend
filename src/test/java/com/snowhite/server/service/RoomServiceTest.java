package com.snowhite.server.service;

import com.snowhite.server.domain.entity.User;
import com.snowhite.server.domain.session.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceTest {

    @Mock
    private ReactiveRedisTemplate<String, Room> reactiveRedisTemplateForRooms;

    @Mock
    private ReactiveValueOperations<String, Room> reactiveValueOperations;

    @InjectMocks
    private RoomService roomService;

    private static final String ROOM_PREFIX = "room:";

    @Test
    void getRooms_shouldReturnRoomsSuccessfully() {
        // given
        Long roomId1 = 1L;
        Long roomId2 = 2L;

        User user = new User();
        List<User> users = List.of(user);

        Room room1 = new Room(roomId1, "Room 1", user, users, 3, 15, false);
        Room room2 = new Room(roomId2, "Room 2", user, users, 5, 30, false);
        List<Room> expectedRooms = Arrays.asList(room1, room2);

        // scanRoomKeys() mocking
        RoomService spyService = spy(roomService);
        doReturn(Flux.just(ROOM_PREFIX + String.valueOf(roomId1), ROOM_PREFIX + String.valueOf(roomId2))).when(spyService).scanRoomKeys();

        //Redis template get mocking
        when(reactiveRedisTemplateForRooms.opsForValue()).thenReturn(reactiveValueOperations);

        // Redis value get mocking
        when(reactiveValueOperations.get(ROOM_PREFIX + String.valueOf(roomId1))).thenReturn(Mono.just(room1));
        when(reactiveValueOperations.get(ROOM_PREFIX + String.valueOf(roomId2))).thenReturn(Mono.just(room2));

        // when & then
        StepVerifier.create(spyService.getRooms())
                .expectNextMatches(response ->
                        response.roomList().equals(expectedRooms)
                )
                .verifyComplete();

        verify(reactiveValueOperations).get(ROOM_PREFIX + String.valueOf(roomId1));
        verify(reactiveValueOperations).get(ROOM_PREFIX + String.valueOf(roomId2));
    }

    @Test
    void getRooms_shouldReturnEmptyListWhenNoRooms() {
        // given
        RoomService spyService = spy(roomService);
        doReturn(Flux.empty()).when(spyService).scanRoomKeys();

        // when & then
        StepVerifier.create(spyService.getRooms())
                .expectNextMatches(response ->
                        response.roomList().isEmpty()
                )
                .verifyComplete();
    }

}
