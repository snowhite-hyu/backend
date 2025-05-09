package com.snowhite.server.web.service;

import com.snowhite.server.domain.Room;
import com.snowhite.server.domain.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class LobbyHandlerTest {

    @Test
     void getRooms_returnsRoomList() {

        //given
        ReactiveRedisTemplate<String, Long> redisTemplateForIds = Mockito.mock(ReactiveRedisTemplate.class);
        ReactiveRedisTemplate<Long, Room> redisTemplateForRooms = Mockito.mock(ReactiveRedisTemplate.class);

        var setOps = Mockito.mock(ReactiveSetOperations.class);
        var valueOps = Mockito.mock(ReactiveValueOperations.class);

        Mockito.when(redisTemplateForIds.opsForSet()).thenReturn(setOps);
        Mockito.when(redisTemplateForRooms.opsForValue()).thenReturn(valueOps);

        Long roomId1 = 1L;
        Long roomId2 = 2L;

        User user1 = new User();
        user1.setId(1);
        User user2 = new User();
        user2.setId(2);

        User user3 = new User();
        user3.setId(3);
        User user4 = new User();
        user4.setId(4);

        Set<Long> roomIds = Set.of(roomId1, roomId2);

        Room room1 = new Room(1L, user1, List.of(user1, user2), 10, 30, false);
        Room room2 = new Room(2L, user3, List.of(user3, user4), 20, 30, true);

        Mockito.when(setOps.members("rooms")).thenReturn(Flux.fromIterable(roomIds));
        Mockito.when(valueOps.get(roomId1)).thenReturn(Mono.just(room1));
        Mockito.when(valueOps.get(roomId2)).thenReturn(Mono.just(room2));

        LobbyHandler handler = new LobbyHandler(redisTemplateForIds, redisTemplateForRooms);

        RouterFunction<?> router = RouterFunctions.route()
                .GET("/rooms", handler::getRooms)
                .build();

        WebTestClient client = WebTestClient.bindToRouterFunction(router).build();

        //when
        client.get().uri("/rooms")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()

        //then
                .expectStatus().isOk()
                .expectBodyList(Room.class)
                .consumeWith(response -> {

                   List<Room> rooms = response.getResponseBody();
                   assertNotNull(rooms);
                   assertEquals(2, rooms.size());

                   Room foundRoom = rooms.stream()
                           .filter(r -> r.getRoomId().equals(room1.getRoomId()))
                           .findAny()
                           .orElseThrow();

                   List<User> expectedUsers = room1.getUsers();
                   List<User> actualUsers = foundRoom.getUsers();
                   assertEquals(expectedUsers.size(), actualUsers.size());
                   for (int i = 0; i < expectedUsers.size(); i++) {
                      assertEquals(expectedUsers.get(i).getId(), actualUsers.get(i).getId());
                   }

                   assertEquals(room1.getRoomId(), foundRoom.getRoomId());

                   boolean containsUser1 = actualUsers.stream().anyMatch(u -> u.getId() == user1.getId());
                   assertTrue(containsUser1);
                });
    }
}