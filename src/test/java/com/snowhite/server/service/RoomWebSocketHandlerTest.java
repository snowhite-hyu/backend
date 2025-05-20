package com.snowhite.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.snowhite.server.domain.session.Room;
import com.snowhite.server.domain.entity.User;
import com.snowhite.server.repository.UserRepository;
import com.snowhite.server.security.jwt.JwtProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.web.reactive.socket.WebSocketMessage;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoomWebSocketHandlerTest {

    private boolean isJson(String msg) {
        try {
            JsonNode node = objectMapper.readTree(msg);
            return node.has("roomId");
        } catch (IOException e) {
            return false;
        }
    }


    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Qualifier("reactiveRedisTemplateForRooms")
    private ReactiveRedisTemplate<String, Room> redisTemplateForRooms;


    @LocalServerPort
    private int port;

    private WebSocketClient client;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setup(@Autowired PasswordEncoder passwordEncoder) {
        client = new ReactorNettyWebSocketClient();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setEmail("testuser@test.com");
        testUser.setLoggedIn(true);
        testUser = userRepository.save(testUser);

        jwtToken = jwtProvider.generateToken(testUser.getId());
    }

    @AfterEach
    void tearDown() {



        userRepository.deleteAll();

        redisTemplateForRooms.keys("*")
                .flatMap(redisTemplateForRooms::delete)
                .then()
                .block();
    }

    @Test
    void testCreateRoom() throws Exception {
        String uri = "ws://localhost:" + port + "/rooms?token=" + jwtToken;

        CountDownLatch latch = new CountDownLatch(1);

        client.execute(
                URI.create(uri),
                session -> {
                    ObjectNode payload = objectMapper.createObjectNode();
                    payload.put("action", "create");
                    payload.put("capacity", 4);
                    payload.put("turnTime", 30);

                    session.send(Mono.just(session.textMessage(payload.toString()))).subscribe();

                    return session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .doOnNext(message -> {

                                try {
                                    JsonNode node = objectMapper.readTree(message);

                                    Assertions.assertEquals(4, node.get("capacity").asInt());
                                    Assertions.assertEquals(30, node.get("turnTime").asInt());
                                    Assertions.assertEquals(false, node.get("playing").asBoolean());

                                    JsonNode masterPlayerNode = node.get("masterPlayer");
                                    Assertions.assertEquals(testUser.getId(), masterPlayerNode.get("id").asLong());
                                    Assertions.assertEquals(testUser.getUsername(), masterPlayerNode.get("username").asText());
                                    Assertions.assertEquals(testUser.getEmail(), masterPlayerNode.get("email").asText());
                                    Assertions.assertEquals(testUser.isLoggedIn(), masterPlayerNode.get("loggedIn").asBoolean());

                                    session.close().subscribe();
                                    latch.countDown();
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                            })
                            .then();
                }
        ).block(Duration.ofSeconds(5));

        if (!latch.await(5, TimeUnit.SECONDS)) {
            Assertions.fail("Did not receive response from WebSocket server");
        }
    }


    @Test
    void testJoinRoomAndBroadcast() throws Exception {

        String roomId = "room:1";

        User joinUser = new User();
        joinUser.setUsername("joinUser");
        joinUser.setEmail("joinUser@example.com");
        joinUser.setPassword("password");
        joinUser.setLoggedIn(true);

        userRepository.save(joinUser);

        String createUri = "ws://localhost:" + port + "/rooms?token=" + jwtToken;
        String joinUri = "ws://localhost:" + port + "/rooms?token=" + jwtProvider.generateToken(joinUser.getId());

        Thread hostThread = new Thread(() -> {
            client.execute(
                    URI.create(createUri),
                    session -> {
                        ObjectNode payload = objectMapper.createObjectNode();
                        payload.put("action", "create");
                        payload.put("capacity", 4);
                        payload.put("turnTime", 30);

                        session.send(Mono.just(session.textMessage(payload.toString()))).subscribe();

                        return session.receive()
                                .map(WebSocketMessage::getPayloadAsText)
                                .doOnNext(msg -> {
                                    if (isJson(msg)) {
                                        try {
                                            Room room = objectMapper.readValue(msg, Room.class);
                                            System.out.println("when user create room, host received: " + room);
                                            Assertions.assertEquals(roomId, room.getRoomId());
                                            Assertions.assertEquals(testUser.getId(), room.getMasterPlayer().getId());
                                        } catch (JsonProcessingException e) {
                                            throw new RuntimeException(e);
                                        }
                                    } else {
                                        System.out.println("when user is joined, host received: " + msg);
                                    }
                                })
                                .take(2)
                                .then();
                    }
            ).block();
        });

        hostThread.start();

        Thread.sleep(3000);

        client.execute(
                URI.create(joinUri),
                session -> {
                    ObjectNode payload = objectMapper.createObjectNode();
                    payload.put("action", "join");
                    payload.put("roomId", roomId);

                    return session.send(Mono.just(session.textMessage(payload.toString())))
                            .thenMany(session.receive()
                                    .map(WebSocketMessage::getPayloadAsText)
                                    .doOnNext(msg -> {
                                                if (isJson(msg)) {
                                                    try {
                                                        Room room = objectMapper.readValue(msg, Room.class);
                                                        System.out.println("when user join room, join user received: " + room);
                                                        Assertions.assertEquals(roomId, room.getRoomId());
                                                        Assertions.assertEquals(testUser.getId(), room.getMasterPlayer().getId());
                                                        Assertions.assertTrue(room.getUsers().stream().anyMatch(
                                                                user -> user.getId() == joinUser.getId())
                                                        );
                                                    } catch (JsonProcessingException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                }
                                                else {
                                                    System.out.println("when user is joined, join user received: " + msg);
                                                }
                                            })
                            )
                            .take(2)
                            .then();
                }
        ).block();

    }

}
