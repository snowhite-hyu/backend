package com.snowhite.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.snowhite.server.config.JwtProvider;
import com.snowhite.server.domain.Room;
import com.snowhite.server.domain.User;
import com.snowhite.server.domain.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.web.reactive.socket.WebSocketMessage;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoomWebSocketServiceTest {

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;
    private String jwtToken;

    @BeforeAll
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
                            .take(1)
                            .then();
                }
        ).block(Duration.ofSeconds(5));

        if (!latch.await(5, TimeUnit.SECONDS)) {
            Assertions.fail("Did not receive response from WebSocket server");
        }
    }
}
