package com.snowhite.server.service;

import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.session.Game;
import com.snowhite.server.domain.session.Player;
import com.snowhite.server.init.CardInitializer;
import com.snowhite.server.repository.CardRepository;
import com.snowhite.server.websocket.dto.request.RockfallCardUseRequest;
import com.snowhite.server.websocket.dto.response.RockfallCardUsedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class UseActionCardServiceTest {

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
    void RockFallCardSetUp() {

        reactiveRedisTemplateForGame = Mockito.mock(ReactiveRedisTemplate.class);
        valueOperationsForGame = Mockito.mock(ReactiveValueOperations.class);
        reactiveRedisTemplateForCard = Mockito.mock(ReactiveRedisTemplate.class);
        valueOperationsForCard = Mockito.mock(ReactiveValueOperations.class);

        when(reactiveRedisTemplateForGame.opsForValue()).thenReturn(valueOperationsForGame);
        when(reactiveRedisTemplateForCard.opsForValue()).thenReturn(valueOperationsForCard);

        gameService = new GameService(reactiveRedisTemplateForGame, reactiveRedisTemplateForCard);
        List<Player> players = List.of(player1, player2, player3, player4, player5);
        game = new Game(GAME_ID, players, TURN_TIME);
        game.clearField();
        game.getField()[5][5][0] = 10;
        player1.addCardToHand(111);

        CardInitializer initializer = new CardInitializer(cardRepository, reactiveRedisTemplateForCard);

        when(valueOperationsForGame.get(eq(GAME_PREFIX + GAME_ID))).thenReturn(Mono.just(game));
        when(valueOperationsForGame.set(eq(GAME_PREFIX + GAME_ID), any(Game.class))).thenReturn(Mono.just(true));
        initializer.initializeCards();
    }

    @Test
    void rockfallCardSuccessTest() {
        RockfallCardUseRequest request = new RockfallCardUseRequest(
                GAME_ID, player1.getPlayerId(), 111, 5,5);
        Mono<RockfallCardUsedResponse> response = gameService.useRockfallCard(request);

        StepVerifier.create(response)
                .assertNext(res -> {
                    assertFalse(res.playerHand().contains(111));
                    assertEquals(-1, game.getField()[5][5][0]);
                })
                .verifyComplete();
    }
}
