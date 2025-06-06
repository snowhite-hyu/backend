package com.snowhite.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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

import java.time.Duration;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.reactive.socket.WebSocketMessage;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoomWebSocketHandlerTest {

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReactiveRedisTemplate<String, Room> redisTemplateForRooms;

    @LocalServerPort
    private int port;

    private WebSocketClient client;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String jwtToken;

    private static final String ROOM_PREFIX = "room:";

    private static final AtomicLong roomIdGenerator = new AtomicLong(0);

    @BeforeEach
    void setup(@Autowired PasswordEncoder passwordEncoder) {
        client = new ReactorNettyWebSocketClient();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setEmail("testuser@test.com");
        testUser = userRepository.save(testUser);

        jwtToken = jwtProvider.generateToken(testUser.getId());
    }

    @AfterEach
    void tearDown() {

        userRepository.deleteAll();

        redisTemplateForRooms.keys("room:*")
                .flatMap(redisTemplateForRooms::delete)
                .then()
                .block();
    }

    @Test
    void testCreateRoom() throws Exception {
        String uri = "ws://localhost:" + port + "/ws/room?token=" + jwtToken;
        Long roomId = roomIdGenerator.incrementAndGet();

        client.execute(
                URI.create(uri),
                session -> {

                    ObjectNode payload = objectMapper.createObjectNode();
                    payload.put("capacity", 4);
                    payload.put("turnTime", 30);
                    payload.put("roomName", "testRoom");

                    ObjectNode request = objectMapper.createObjectNode();
                    request.put("type", "create");
                    request.set("payload", payload);

                    session.send(Mono.just(session.textMessage(request.toString()))).subscribe();


                    return session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .doOnNext(message -> {

                                try {
                                    System.out.println(message);

                                    JsonNode root = objectMapper.readTree(message);

                                    Assertions.assertEquals("created-room", root.get("type").asText());

                                    JsonNode payloadNode = root.get("payload");
                                    Assertions.assertNotNull(payloadNode);



                                    Long getRoomId = payloadNode.get("roomId").asLong();
                                    String roomName = payloadNode.get("roomName").asText();
                                    JsonNode masterPlayer = payloadNode.get("masterPlayer");
                                    JsonNode users = payloadNode.get("users");
                                    int capacity = payloadNode.get("capacity").asInt();
                                    int turnTime = payloadNode.get("turnTime").asInt();
                                    boolean isPlaying = payloadNode.get("playing").asBoolean();

                                    Assertions.assertEquals(roomId, getRoomId);
                                    Assertions.assertEquals("testRoom", roomName);
                                    Assertions.assertEquals(4, capacity);
                                    Assertions.assertEquals(30, turnTime);
                                    Assertions.assertFalse(isPlaying);

                                    Assertions.assertEquals(testUser.getId(), masterPlayer.get("id").asLong());
                                    Assertions.assertEquals(testUser.getUsername(), masterPlayer.get("username").asText());

                                    Assertions.assertTrue(users.isArray());
                                    Assertions.assertEquals(1, users.size());
                                    Assertions.assertEquals(masterPlayer, users.get(0));

                                    session.close().subscribe();

                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                            })
                            .then();
                }
        ).block();

    }

    @Test
    void testErrorWhenInvalidPayload() throws Exception {
        String uri = "ws://localhost:" + port + "/ws/room?token=" + jwtToken;

        client.execute(
                URI.create(uri),
                session -> {

                    ObjectNode payload = objectMapper.createObjectNode();
                    payload.put("capacity", 4);
                    payload.put("turnTime", 30);

                    ObjectNode request = objectMapper.createObjectNode();
                    request.put("type", "create");
                    request.set("payload", payload);

                    session.send(Mono.just(session.textMessage(request.toString()))).subscribe();


                    return session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .doOnNext(message -> {

                                try {

                                    System.out.println("Received: " + message);

                                    JsonNode root = objectMapper.readTree(message);
                                    String type = root.get("type").asText();

                                    Assertions.assertEquals("error", root.get("type").asText());

                                    String errorMsg = root.get("payload").asText();
                                    Assertions.assertTrue(
                                            errorMsg.contains("Invalid Websocket frame"),
                                            "Actual error: " + errorMsg
                                    );


                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .take(1)
                            .then();
                }
        ).block();

    }

    @Test
    void testErrorWhenRoomNotExist() throws Exception {
        Long roomId = roomIdGenerator.incrementAndGet();

        User joinUser = new User();
        joinUser.setUsername("joinUser");
        joinUser.setEmail("joinUser@example.com");
        joinUser.setPassword("password");

        userRepository.save(joinUser);

        String createUri = "ws://localhost:" + port + "/ws/room?token=" + jwtToken;
        String joinUri = "ws://localhost:" + port + "/ws/room?token=" + jwtProvider.generateToken(joinUser.getId());

        Thread hostThread = new Thread(() -> {
            client.execute(
                    URI.create(createUri),
                    session -> {

                        ObjectNode payload = objectMapper.createObjectNode();
                        payload.put("capacity", 4);
                        payload.put("turnTime", 30);
                        payload.put("roomName", "testRoom");

                        ObjectNode request = objectMapper.createObjectNode();
                        request.put("type", "create");
                        request.set("payload", payload);

                        session.send(Mono.just(session.textMessage(request.toString()))).subscribe();

                        return session.receive()
                                .map(WebSocketMessage::getPayloadAsText)
                                .doOnNext(msg -> {
                                    try {
                                        JsonNode root = objectMapper.readTree(msg);
                                        String type = root.get("type").asText();

                                        if (type.equals("created-room")) {
                                            JsonNode payloadNode = root.get("payload");
                                            Assertions.assertNotNull(payloadNode);

                                            Assertions.assertEquals(roomId, payloadNode.get("roomId").asLong());
                                            Assertions.assertEquals(testUser.getId(), payloadNode.get("masterPlayer").get("id").asLong());
                                        }
                                        else if (type.equals("room-users")) {

                                            System.out.println(msg);

                                            JsonNode payloadNode = root.get("payload");
                                            Assertions.assertNotNull(payloadNode);
                                            Assertions.assertTrue(payloadNode.isArray());

                                            List<User> users = objectMapper.readValue(
                                                    payloadNode.toString(),
                                                    new TypeReference<List<User>>() {}
                                            );

                                            Assertions.assertTrue(
                                                    users.stream().anyMatch(u -> u.getId() == testUser.getId())
                                            );
                                        }
                                        else {Assertions.fail("unexpected type: " + type);}

                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .take(1)
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
                    payload.put("roomId", roomId+1L);

                    ObjectNode request = objectMapper.createObjectNode();
                    request.put("type", "join");
                    request.set("payload", payload);

                    return session.send(Mono.just(session.textMessage(request.toString())))
                            .thenMany(session.receive()
                                    .map(WebSocketMessage::getPayloadAsText)
                                    .doOnNext(msg -> {
                                        try {

                                            System.out.println("Received: " + msg);

                                            JsonNode root = objectMapper.readTree(msg);
                                            String type = root.get("type").asText();

                                            Assertions.assertEquals("error", root.get("type").asText());

                                            String errorMsg = root.get("payload").asText();
                                            Assertions.assertTrue(
                                                    errorMsg.contains("Room is not found"),
                                                    "Actual error: " + errorMsg
                                            );

                                        } catch (JsonProcessingException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                            )
                            .take(1)
                            .then();
                }
        ).block();
    }


    @Test
    void testJoinRoomAndBroadcast() throws Exception {

        Long roomId = roomIdGenerator.incrementAndGet();

        User joinUser = new User();
        joinUser.setUsername("joinUser");
        joinUser.setEmail("joinUser@example.com");
        joinUser.setPassword("password");

        userRepository.save(joinUser);

        String createUri = "ws://localhost:" + port + "/ws/room?token=" + jwtToken;
        String joinUri = "ws://localhost:" + port + "/ws/room?token=" + jwtProvider.generateToken(joinUser.getId());

        Thread hostThread = new Thread(() -> {
            client.execute(
                    URI.create(createUri),
                    session -> {

                        ObjectNode payload = objectMapper.createObjectNode();
                        payload.put("capacity", 4);
                        payload.put("turnTime", 30);
                        payload.put("roomName", "testRoom");

                        ObjectNode request = objectMapper.createObjectNode();
                        request.put("type", "create");
                        request.set("payload", payload);

                        session.send(Mono.just(session.textMessage(request.toString()))).subscribe();

                        return session.receive()
                                .map(WebSocketMessage::getPayloadAsText)
                                .doOnNext(msg -> {
                                        try {
                                            JsonNode root = objectMapper.readTree(msg);
                                            String type = root.get("type").asText();

                                            if (type.equals("created-room")) {
                                                JsonNode payloadNode = root.get("payload");
                                                Assertions.assertNotNull(payloadNode);

                                                Assertions.assertEquals(roomId, payloadNode.get("roomId").asLong());
                                                Assertions.assertEquals(testUser.getId(), payloadNode.get("masterPlayer").get("id").asLong());
                                            }
                                            else if (type.equals("room-users")) {

                                                System.out.println(msg);

                                                JsonNode payloadNode = root.get("payload");
                                                Assertions.assertNotNull(payloadNode);
                                                Assertions.assertTrue(payloadNode.isArray());

                                                List<User> users = objectMapper.readValue(
                                                        payloadNode.toString(),
                                                        new TypeReference<List<User>>() {}
                                                );

                                                Assertions.assertTrue(
                                                        users.stream().anyMatch(u -> u.getId() == testUser.getId())
                                                );
                                            }
                                            else {Assertions.fail("unexpected type: " + type);}

                                        } catch (JsonProcessingException e) {
                                            throw new RuntimeException(e);
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
                    payload.put("roomId", roomId);

                    ObjectNode request = objectMapper.createObjectNode();
                    request.put("type", "join");
                    request.set("payload", payload);

                    return session.send(Mono.just(session.textMessage(request.toString())))
                            .thenMany(session.receive()
                                    .map(WebSocketMessage::getPayloadAsText)
                                    .doOnNext(msg -> {
                                        try {

                                            System.out.println(msg);

                                            JsonNode root = objectMapper.readTree(msg);
                                            String type = root.get("type").asText();

                                            if (type.equals("joined-room")) {
                                                JsonNode payloadNode = root.get("payload");
                                                Assertions.assertNotNull(payloadNode);

                                                Assertions.assertEquals(roomId, payloadNode.get("roomId").asLong());

                                                List<User> users = objectMapper.readValue(
                                                        payloadNode.get("users").toString(),
                                                        new TypeReference<List<User>>() {}
                                                );

                                                Assertions.assertTrue(
                                                        users.stream().anyMatch(u -> u.getId() == joinUser.getId())
                                                );


                                            }
                                            else {Assertions.fail("unexpected type: " + type);}

                                        } catch (JsonProcessingException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                            )
                            .take(1)
                            .then();
                }
        ).block();
    }

    @Test
    void testQuitRoomAndBroadcast() throws Exception {

        Long roomId = roomIdGenerator.incrementAndGet();

        User joinUser = new User();
        joinUser.setUsername("joinUser");
        joinUser.setEmail("joinUser@example.com");
        joinUser.setPassword("password");
        userRepository.save(joinUser);

        String hostUri = "ws://localhost:" + port + "/ws/room?token=" + jwtToken;
        String joinUri = "ws://localhost:" + port + "/ws/room?token=" + jwtProvider.generateToken(joinUser.getId());

        Thread hostThread = new Thread(() -> {
            client.execute(
                    URI.create(hostUri),
                    session -> {

                        ObjectNode payload = objectMapper.createObjectNode();
                        payload.put("capacity", 4);
                        payload.put("turnTime", 30);
                        payload.put("roomName", "testRoom");

                        ObjectNode request = objectMapper.createObjectNode();
                        request.put("type", "create");
                        request.set("payload", payload);

                        session.send(Mono.just(session.textMessage(request.toString()))).subscribe();

                        return session.receive()
                                .map(WebSocketMessage::getPayloadAsText)
                                .doOnNext(msg -> {
                                    try {
                                        JsonNode root = objectMapper.readTree(msg);
                                        String type = root.get("type").asText();

                                        if (type.equals("created-room")) {
                                            JsonNode payloadNode = root.get("payload");
                                            Assertions.assertNotNull(payloadNode);

                                            Assertions.assertEquals(roomId, payloadNode.get("roomId").asLong());
                                            Assertions.assertEquals(testUser.getId(), payloadNode.get("masterPlayer").get("id").asLong());
                                        }
                                        else if (type.equals("room-users")) {

                                            System.out.println("host received: " + msg);

                                            JsonNode payloadNode = root.get("payload");
                                            Assertions.assertNotNull(payloadNode);
                                            Assertions.assertTrue(payloadNode.isArray());

                                            List<User> users = objectMapper.readValue(
                                                    payloadNode.toString(),
                                                    new TypeReference<List<User>>() {}
                                            );

                                            Assertions.assertTrue(
                                                    users.stream().anyMatch(u -> u.getId() == testUser.getId())
                                            );
                                        }
                                        else {Assertions.fail("unexpected type: " + type);}

                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .take(3)
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
                    payload.put("roomId", roomId);

                    ObjectNode request = objectMapper.createObjectNode();
                    request.put("type", "join");
                    request.set("payload", payload);

                    return session.send(Mono.just(session.textMessage(request.toString())))
                            .thenMany(session.receive()
                                    .map(WebSocketMessage::getPayloadAsText)
                                    .doOnNext(msg -> {
                                        try {

                                            System.out.println("join received: " + msg);

                                            JsonNode root = objectMapper.readTree(msg);
                                            String type = root.get("type").asText();

                                            if (type.equals("joined-room")) {
                                                JsonNode payloadNode = root.get("payload");
                                                Assertions.assertNotNull(payloadNode);

                                                Assertions.assertEquals(roomId, payloadNode.get("roomId").asLong());

                                                List<User> users = objectMapper.readValue(
                                                        payloadNode.get("users").toString(),
                                                        new TypeReference<List<User>>() {}
                                                );

                                                Assertions.assertTrue(
                                                        users.stream().anyMatch(u -> u.getId() == joinUser.getId())
                                                );


                                            }
                                            else {Assertions.fail("unexpected type: " + type);}

                                        } catch (JsonProcessingException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                                    .take(1)
                            )
                            .then();
                }
        ).block();

        client.execute(
                URI.create(joinUri),
                session -> {
                    ObjectNode payload = objectMapper.createObjectNode();
                    payload.put("roomId", roomId);

                    ObjectNode request = objectMapper.createObjectNode();
                    request.put("type", "quit");
                    request.set("payload", payload);

                    return session.send(Mono.just(session.textMessage(request.toString())))
                            .thenMany(session.receive()
                                    .map(WebSocketMessage::getPayloadAsText)
                                    .doOnSubscribe(sub -> System.out.println("Subscribed!"))
                                    .doOnNext(msg -> {
                                        try {

                                            System.out.println("quit received: " + msg);

                                            JsonNode root = objectMapper.readTree(msg);
                                            String type = root.get("type").asText();

                                            if (type.equals("quit-success")) {
                                                JsonNode payloadNode = root.get("payload");
                                                Assertions.assertNull(payloadNode);
                                            }
                                            else {Assertions.fail("unexpected type: " + type);}

                                        } catch (JsonProcessingException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                                    .doOnComplete(() -> System.out.println("Completed!"))
                                    .take(1)
                                    .timeout(Duration.ofSeconds(10))
                            )
                            .then();
                }
        ).block();

    }

    @Test
    void testChatBroadcastBetweenUsers() throws Exception {
        Long roomId = roomIdGenerator.incrementAndGet();

        // Create and save a second user
        User joinUser = new User();
        joinUser.setUsername("joinUser");
        joinUser.setEmail("joinUser@example.com");
        joinUser.setPassword("password");
        userRepository.save(joinUser);

        String hostUri = "ws://localhost:" + port + "/ws/room?token=" + jwtToken;
        String joinUri = "ws://localhost:" + port + "/ws/room?token=" + jwtProvider.generateToken(joinUser.getId());

        // Host thread: create room and wait for chat message
        Thread hostThread = new Thread(() -> {
            client.execute(
                    URI.create(hostUri),
                    session -> {
                        ObjectNode payload = objectMapper.createObjectNode();
                        payload.put("capacity", 4);
                        payload.put("turnTime", 30);
                        payload.put("roomName", "testRoom");

                        ObjectNode request = objectMapper.createObjectNode();
                        request.put("type", "create");
                        request.set("payload", payload);

                        session.send(Mono.just(session.textMessage(request.toString()))).subscribe();

                        // Wait for chat message after room creation and join
                        return session.receive()
                                .map(WebSocketMessage::getPayloadAsText)
                                .doOnNext(msg -> {
                                    try {
                                        JsonNode root = objectMapper.readTree(msg);
                                        String type = root.get("type").asText();
                                        if (type.equals("chat")) {

                                            System.out.println("Host user received chat: " + msg);

                                            JsonNode payloadNode = root.get("payload");
                                            Assertions.assertNotNull(payloadNode);
                                            Assertions.assertEquals("Hello, world!", payloadNode.get("message").asText());
                                            JsonNode userNode = payloadNode.get("user");
                                            Assertions.assertNotNull(userNode);
                                            Assertions.assertEquals(joinUser.getId(), userNode.get("id").asLong());
                                            Assertions.assertEquals(joinUser.getUsername(), userNode.get("username").asText());
                                        }
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .filter(msg -> {
                                    try {
                                        return objectMapper.readTree(msg).get("type").asText().equals("chat");
                                    } catch (Exception e) { return false; }
                                })
                                .take(1)
                                .then();
                    }
            ).block();
        });

        hostThread.start();
        Thread.sleep(3000);

        // Join user: join room, then send chat message
        client.execute(
                URI.create(joinUri),
                session -> {
                    ObjectNode joinPayload = objectMapper.createObjectNode();
                    joinPayload.put("roomId", roomId);
                    ObjectNode joinRequest = objectMapper.createObjectNode();
                    joinRequest.put("type", "join");
                    joinRequest.set("payload", joinPayload);

                    // Send join request
                    session.send(Mono.just(session.textMessage(joinRequest.toString()))).subscribe();

                    // Wait a bit, then send chat message
                    return session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .doOnNext(msg -> {
                                try {
                                    JsonNode root = objectMapper.readTree(msg);
                                    if (root.get("type").asText().equals("joined-room")) {
                                        // Send chat message after join
                                        ObjectNode chatPayload = objectMapper.createObjectNode();
                                        chatPayload.put("roomId", roomId);
                                        chatPayload.put("message", "Hello, world!");
                                        ObjectNode chatRequest = objectMapper.createObjectNode();
                                        chatRequest.put("type", "chat");
                                        chatRequest.set("payload", chatPayload);
                                        session.send(Mono.just(session.textMessage(chatRequest.toString()))).subscribe();
                                    }
                                } catch (Exception e) { }
                            })
                            .take(1)
                            .then();
                }
        ).block();

        hostThread.join();
    }

}
