package com.snowhite.server.service;

import com.snowhite.server.domain.session.Room;
import com.snowhite.server.domain.entity.User;
import com.snowhite.server.payload.ApiResponse;
import com.snowhite.server.web.dto.web.response.GetRoomResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LobbyServiceTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    @Qualifier("reactiveRedisTemplateForRooms")
    private ReactiveRedisTemplate<String, Room> redisTemplateForRooms;

    @Autowired
    private WebTestClient webTestClient;

    private static final String ROOM_PREFIX = "room:";

    @BeforeEach
    void setUp() {

        Long roomId1 = 3L;
        Long roomId2 = 17L;

        User user1 = new User();
        user1.setId(1);
        User user2 = new User();
        user2.setId(2);

        User user3 = new User();
        user3.setId(3);
        User user4 = new User();
        user4.setId(4);

        Room room1 = new Room(roomId1, user1, List.of(user1, user2), 10, 30, false);
        Room room2 = new Room(roomId2, user3, List.of(user3, user4), 20, 30, true);

        redisTemplateForRooms.opsForValue().set(ROOM_PREFIX + String.valueOf(roomId1), room1).block();
        redisTemplateForRooms.opsForValue().set(ROOM_PREFIX + String.valueOf(roomId2), room2).block();

    }

    @AfterEach
    void tearDown() {
        redisTemplateForRooms.keys("room:*")
                .flatMap(redisTemplateForRooms::delete)
                .then()
                .block();
    }



    @Test
     void getRooms_returnsRoomList() {

        webTestClient.get().uri("/api/rooms")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<GetRoomResponse>>() {})
                .consumeWith(response -> {

                    System.out.println(response);

                    ApiResponse<GetRoomResponse> apiResponse = response.getResponseBody();

                    assertNotNull(apiResponse);
                    assertTrue(apiResponse.getIsSuccess());
                    assertEquals("COMMON200", apiResponse.getCode());

                    List<Room> rooms = apiResponse.getResult().roomList();
                    assertNotNull(rooms);
                    assertEquals(2, rooms.size());

                    Room foundRoom = rooms.stream()
                            .filter(r -> r.getRoomId().equals(3L))
                            .findAny()
                            .orElseThrow();

                    User user1 = new User();
                    user1.setId(1);
                    User user2 = new User();
                    user2.setId(2);

                    List<User> expectedUsers = List.of(user1, user2);
                    List<User> actualUsers = foundRoom.getUsers();
                    assertEquals(expectedUsers.size(), actualUsers.size());

                    for (int i = 0; i < expectedUsers.size(); i++) {
                        assertEquals(expectedUsers.get(i).getId(), actualUsers.get(i).getId());
                    }

                    assertEquals(3L, foundRoom.getRoomId());

                    boolean containsUser1 = actualUsers.stream().anyMatch(u -> u.getId() == 1);
                    assertTrue(containsUser1);
                });
    }

    @Test
    void getRooms_returnsEmptyList_whenNoRoomsExist() {

        redisTemplateForRooms.keys("room:*")
                .flatMap(redisTemplateForRooms::delete)
                .blockLast();

        webTestClient.get().uri("/api/rooms")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<GetRoomResponse>>() {})
                .consumeWith(response -> {
                    ApiResponse<GetRoomResponse> apiResponse = response.getResponseBody();

                    assertNotNull(apiResponse);
                    assertTrue(apiResponse.getIsSuccess());
                    assertEquals("COMMON200", apiResponse.getCode());

                    GetRoomResponse rooms = apiResponse.getResult();
                    assertNotNull(rooms);
                    assertEquals(0, rooms.roomList().size());
                });
    }

}