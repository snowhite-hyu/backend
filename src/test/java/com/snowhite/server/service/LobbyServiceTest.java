package com.snowhite.server.service;

import com.snowhite.server.domain.Room;
import com.snowhite.server.domain.User;
import com.snowhite.server.payload.ApiResponse;
import com.snowhite.server.service.LobbyService;
import com.snowhite.server.web.controller.LobbyController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
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


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LobbyServiceTest {

    @Autowired
    private WebTestClient client;

    @Autowired()
    @Qualifier("reactiveRedisTemplateForRooms")
    private ReactiveRedisTemplate<String, Room> redisTemplateForRooms;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {

        String roomId1 = "room:" + "003";
        String roomId2 = "room:" + "017";

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

        redisTemplateForRooms.opsForValue().set(roomId1, room1).block();
        redisTemplateForRooms.opsForValue().set(roomId2, room2).block();

    }

    @AfterEach
    void tearDown() {
        redisTemplateForRooms.delete("room:003").block();
        redisTemplateForRooms.delete("room:017").block();
    }



    @Test
     void getRooms_returnsRoomList() {

        webTestClient.get().uri("/lobby")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<List<Room>>>() {})
                .consumeWith(response -> {
                    ApiResponse<List<Room>> apiResponse = response.getResponseBody();

                    assertNotNull(apiResponse);
                    assertTrue(apiResponse.getIsSuccess());
                    assertEquals("COMMON200", apiResponse.getCode());

                    List<Room> rooms = apiResponse.getResult();
                    assertNotNull(rooms);
                    assertEquals(2, rooms.size());

                    Room foundRoom = rooms.stream()
                            .filter(r -> r.getRoomId().equals("room:003"))
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

                    assertEquals("room:003", foundRoom.getRoomId());

                    boolean containsUser1 = actualUsers.stream().anyMatch(u -> u.getId() == 1);
                    assertTrue(containsUser1);
                });
    }
}