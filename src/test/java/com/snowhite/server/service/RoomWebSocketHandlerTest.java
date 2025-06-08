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
import java.util.concurrent.atomic.AtomicBoolean;
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
    @Qualifier("reactiveRedisTemplateForRooms")
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
                    payload.put("roomId", roomId+10L);

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
    void testChatBroadcastToAllUsers() throws Exception {
        Long roomId = roomIdGenerator.incrementAndGet();

        // Create and save users
        User hostUser = testUser;
        User joinUser1 = new User();
        joinUser1.setUsername("joinUser1");
        joinUser1.setEmail("joinUser1@example.com");
        joinUser1.setPassword("password1");
        userRepository.save(joinUser1);

        User joinUser2 = new User();
        joinUser2.setUsername("joinUser2");
        joinUser2.setEmail("joinUser2@example.com");
        joinUser2.setPassword("password2");
        userRepository.save(joinUser2);

        String hostUri = "ws://localhost:" + port + "/ws/room?token=" + jwtProvider.generateToken(hostUser.getId());
        String joinUri1 = "ws://localhost:" + port + "/ws/room?token=" + jwtProvider.generateToken(joinUser1.getId());
        String joinUri2 = "ws://localhost:" + port + "/ws/room?token=" + jwtProvider.generateToken(joinUser2.getId());

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

                        return session.receive()
                                .map(WebSocketMessage::getPayloadAsText)
                                .filter(msg -> {
                                    try {
                                        return objectMapper.readTree(msg).get("type").asText().equals("chat");
                                    } catch (Exception e) { return false; }
                                })
                                .doOnNext(msg -> {
                                    try {
                                        System.out.println("Host received chat: " + msg);
                                        JsonNode root = objectMapper.readTree(msg);
                                        JsonNode payloadNode = root.get("payload");
                                        Assertions.assertNotNull(payloadNode);
                                        Assertions.assertEquals("Hello, world!", payloadNode.get("message").asText());
                                        JsonNode userNode = payloadNode.get("user");
                                        Assertions.assertNotNull(userNode);
                                        Assertions.assertEquals(joinUser2.getId(), userNode.get("id").asLong());
                                        Assertions.assertEquals(joinUser2.getUsername(), userNode.get("username").asText());
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .take(1)
                                .then();
                    }
            ).block();
        });

        // Join user1 thread: join room and wait for chat message
        Thread joinThread1 = new Thread(() -> {
            client.execute(
                    URI.create(joinUri1),
                    session -> {
                        ObjectNode joinPayload = objectMapper.createObjectNode();
                        joinPayload.put("roomId", roomId);
                        ObjectNode joinRequest = objectMapper.createObjectNode();
                        joinRequest.put("type", "join");
                        joinRequest.set("payload", joinPayload);
                        session.send(Mono.just(session.textMessage(joinRequest.toString()))).subscribe();

                        return session.receive()
                                .map(WebSocketMessage::getPayloadAsText)
                                .doOnNext(
                                        msg -> {
                                            System.out.println("JoinUser1 received: " + msg);
                                        }
                                )
                                .filter(msg -> {
                                    try {
                                        return objectMapper.readTree(msg).get("type").asText().equals("chat");
                                    } catch (Exception e) { return false; }
                                })
                                .doOnNext(msg -> {
                                    try {
                                        System.out.println("JoinUser1 received chat: " + msg);
                                        JsonNode root = objectMapper.readTree(msg);
                                        JsonNode payloadNode = root.get("payload");
                                        Assertions.assertNotNull(payloadNode);
                                        Assertions.assertEquals("Hello, world!", payloadNode.get("message").asText());
                                        JsonNode userNode = payloadNode.get("user");
                                        Assertions.assertNotNull(userNode);
                                        Assertions.assertEquals(joinUser2.getId(), userNode.get("id").asLong());
                                        Assertions.assertEquals(joinUser2.getUsername(), userNode.get("username").asText());
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
        Thread.sleep(1000);

        joinThread1.start();
        Thread.sleep(3000);

        // Join user2: join room, then send chat message
        client.execute(
                URI.create(joinUri2),
                session -> {
                    ObjectNode joinPayload = objectMapper.createObjectNode();
                    joinPayload.put("roomId", roomId);
                    ObjectNode joinRequest = objectMapper.createObjectNode();
                    joinRequest.put("type", "join");
                    joinRequest.set("payload", joinPayload);
                    session.send(Mono.just(session.textMessage(joinRequest.toString()))).subscribe();

                    // Wait for join to complete, then send chat
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
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .take(1)
                            .then();
                }
        ).block();

        hostThread.join();
        joinThread1.join();
    }

    @Test
    void testRoomNotDeletedWhenUser1Quits() throws Exception {
        
        Long roomId = roomIdGenerator.incrementAndGet();

        // 방장(Host)와 유저N 생성 및 저장
        User hostUser = testUser;

        User user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setPassword("password1");
        userRepository.save(user1);

        String hostUri = "ws://localhost:" + port + "/ws/room?token=" + jwtProvider.generateToken(hostUser.getId());
        String user1Uri = "ws://localhost:" + port + "/ws/room?token=" + jwtProvider.generateToken(user1.getId());

        // 방장 스레드: 참가/퇴장 알림 수신
        AtomicBoolean quitReceived = new AtomicBoolean(false);

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

                                        System.out.println("host received: " + msg);

                                        if (type.equals("created-room")) {
                                            // 방 생성 확인
                                            Assertions.assertEquals(roomId, root.get("payload").get("roomId").asLong());
                                        } else if (type.equals("room-users")) {
                                            // 참가자 목록 갱신

                                            if (root.get("payload").size() == 1) {
                                                quitReceived.set(true);

                                                JsonNode payloadNode = root.get("payload");
                                                Assertions.assertNotNull(payloadNode);
                                            }

                                        } else if (type.equals("quit-success")) {
                                            // 방장에게 quit-success가 오면 안됨
                                            Assertions.fail("Host should not receive quit-success for other user");
                                        }
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .takeUntil(msg -> quitReceived.get())
                                .then();
                    }
            ).block();
        });

        hostThread.start();
        Thread.sleep(1000);

        // 유저N: 방 참가 후 일정 시간 뒤 퇴장
        client.execute(
                URI.create(user1Uri),
                session -> {
                    ObjectNode joinPayload = objectMapper.createObjectNode();
                    joinPayload.put("roomId", roomId);
                    ObjectNode joinRequest = objectMapper.createObjectNode();
                    joinRequest.put("type", "join");
                    joinRequest.set("payload", joinPayload);

                    // 참가 후 1초 뒤 퇴장
                    return session.send(Mono.just(session.textMessage(joinRequest.toString())))
                            .thenMany(session.receive()
                                    .map(WebSocketMessage::getPayloadAsText)
                                    .doOnNext(msg -> {
                                        try {
                                            JsonNode root = objectMapper.readTree(msg);
                                            if (root.get("type").asText().equals("joined-room")) {
                                                // 참가 성공 후 1초 뒤 퇴장 요청
                                                Thread.sleep(1000);
                                                ObjectNode quitPayload = objectMapper.createObjectNode();
                                                quitPayload.put("roomId", roomId);
                                                ObjectNode quitRequest = objectMapper.createObjectNode();
                                                quitRequest.put("type", "quit");
                                                quitRequest.set("payload", quitPayload);
                                                session.send(Mono.just(session.textMessage(quitRequest.toString()))).subscribe();
                                            }
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                                    .filter(msg -> {
                                        try {
                                            return objectMapper.readTree(msg).get("type").asText().equals("quit-success");
                                        } catch (Exception e) { return false; }
                                    })
                                    .doOnNext(msg -> {
                                        // 유저N이 quit-success를 받음
                                        System.out.println("User1 received quit-success: " + msg);
                                    })
                                    .take(1)
                            )
                            .then();
                }
        ).block();

        // 방장 스레드가 유저N의 퇴장 알림을 받았는지 확인
        hostThread.join();
        Assertions.assertTrue(quitReceived.get(), "Host did not receive user-quit message");

        // 유저N 퇴장 후 2초 뒤에도 방이 살아있는지 확인
        Thread.sleep(2000);
        Room room = redisTemplateForRooms.opsForValue().get("room:" + roomId).block();
        Assertions.assertNotNull(room, "Room should still exist after user1 quit");
        Assertions.assertEquals(1, room.getUsers().size());
        Assertions.assertEquals(hostUser.getId(), room.getUsers().get(0).getId());
    }

}
