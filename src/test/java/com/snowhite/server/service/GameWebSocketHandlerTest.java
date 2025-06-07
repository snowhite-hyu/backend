package com.snowhite.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.entity.User;
import com.snowhite.server.domain.session.Game;
import com.snowhite.server.domain.session.Player;
import com.snowhite.server.init.CardInitializer;
import com.snowhite.server.repository.CardRepository;
import com.snowhite.server.repository.UserRepository;
import com.snowhite.server.security.jwt.JwtProvider;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.socket.WebSocketMessage;

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GameWebSocketHandlerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketClient client;
    private User testUser;
    private String jwtToken;

    private ReactiveRedisTemplate<String, Game> reactiveRedisTemplateForGame;
    private ReactiveRedisTemplate<String, Card> reactiveRedisTemplateForCard;
    private ReactiveValueOperations<String, Game> valueOperationsForGame;
    private ReactiveValueOperations<String, Card> valueOperationsForCard;

    @InjectMocks
    private GameService gameService;

    private static String GAME_PREFIX = "game:";
    private static long GAME_ID = 1L;
    private static int TURN_TIME = 10;
    private static Player player1 = new Player(10L, "player1");
    private static Player player2 = new Player(20L, "player2");
    private static Player player3 = new Player(30L, "player3");
    private static Player player4 = new Player(40L, "player4");
    private static Player player5 = new Player(50L, "player5");

    private Game game;
    @Mock
    private CardRepository cardRepository;

    @BeforeEach
    void setup(@Autowired PasswordEncoder passwordEncoder) {
        client = new ReactorNettyWebSocketClient();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setEmail("testuser@test.com");
        testUser = userRepository.save(testUser);

        jwtToken = jwtProvider.generateToken(testUser.getId());

        reactiveRedisTemplateForGame = Mockito.mock(ReactiveRedisTemplate.class);
        valueOperationsForGame = Mockito.mock(ReactiveValueOperations.class);
        reactiveRedisTemplateForCard = Mockito.mock(ReactiveRedisTemplate.class);
        valueOperationsForCard = Mockito.mock(ReactiveValueOperations.class);
        when(reactiveRedisTemplateForGame.opsForValue()).thenReturn(valueOperationsForGame);
        when(reactiveRedisTemplateForCard.opsForValue()).thenReturn(valueOperationsForCard);

        gameService = new GameService(reactiveRedisTemplateForGame, reactiveRedisTemplateForCard, null);

        List<Player> players = List.of(player1, player2, player3, player4, player5);
        game = new Game(GAME_ID, players, TURN_TIME);
        game.clearField();

        CardInitializer initializer = new CardInitializer(cardRepository, reactiveRedisTemplateForCard);
        initializer.initializeCards();

        when(valueOperationsForGame.get(eq(GAME_PREFIX + GAME_ID))).thenReturn(Mono.just(game));
        when(valueOperationsForGame.set(eq(GAME_PREFIX + GAME_ID), any(Game.class))).thenReturn(Mono.just(true));
    }

    @Test
    void testHandleUseActionCard() throws Exception {
        String uri = "ws://localhost:" + port + "/ws/game?token=" + jwtToken;

        client.execute(
                URI.create(uri),
                session -> {
                    ObjectNode payload = objectMapper.createObjectNode();
                    payload.put("cardId", 109);
                    payload.put("usePlayerId", player1.getPlayerId());
                    payload.put("targetPlayerId", player2.getPlayerId());
                    payload.putNull("locationX");
                    payload.putNull("locationY");
                    payload.putNull("targetRepairState");

                    ObjectNode request = objectMapper.createObjectNode();
                    request.put("type", "use-action-card");
                    request.set("payload", payload);

                    return session.send(Mono.just(session.textMessage(request.toString())))
                            .thenMany(session.receive()
                                    .map(WebSocketMessage::getPayloadAsText)
                                    .doOnNext(msg -> {
                                        try {
                                            System.out.println("received: " + msg);

                                            JsonNode root = objectMapper.readTree(msg);
                                            String type = root.get("type").asText();

                                            if (type.equals("[Unicast]: Action-Card-Use") || type.equals("[Broadcast]: Action-Card-Use")) {
                                                JsonNode responsePayload = root.get("payload");
                                                Assertions.assertNotNull(responsePayload);
                                                Assertions.assertEquals(GAME_ID, responsePayload.get("gameId").asLong());
                                            } else {
                                                Assertions.fail("Unexpected message type: " + type);
                                            }

                                        } catch (JsonProcessingException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                                    .take(2)
                            )
                            .then();
                }
        ).block();
    }
}
