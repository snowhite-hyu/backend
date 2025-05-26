package com.snowhite.server.service;

import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.enums.PlayerState;
import com.snowhite.server.domain.session.Game;
import com.snowhite.server.domain.session.Player;
import com.snowhite.server.init.CardInitializer;
import com.snowhite.server.repository.CardRepository;
import com.snowhite.server.websocket.dto.request.ActionCardUseRequest;
import com.snowhite.server.websocket.dto.response.ActionCardUsedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class UseActionCardServiceTest {

    @Autowired
    private WebTestClient webTestClient;

    @Mock
    private ReactiveRedisTemplate<String, Game> reactiveRedisTemplateForGame;
    @Mock
    private ReactiveRedisTemplate<String, Card> reactiveRedisTemplateForCard;

    @InjectMocks
    private GameService gameService;

    private static final Long GAME_ID = 1L;
    private static final Integer[][][] FIELD = {
            {{-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}},
            {{-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {61, 0}},
            {{-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}},
            {{0, 0},  {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {62, 0}},
            {{-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}},
            {{-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {63, 0}},
            {{-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}, {-1, 0}}
    };
    private static int TURNTIME = 10;
    private static Player player1 = new Player(10L, "player1");
    private static Player player2 = new Player(20L, "player2");
    private static Player player3 = new Player(30L, "player3");
    private static Player player4 = new Player(40L, "player4");
    private static Player player5 = new Player(50L, "player5");
    private static final List<Player> PLAYERS = List.of(player1, player2, player3, player4, player5);

    private Game game;

    @Mock
    private CardRepository cardRepository;
    @Mock
    CardInitializer initializer = new CardInitializer(cardRepository, reactiveRedisTemplateForCard);

    @Mock
    private ReactiveValueOperations<String, Game> valueOperations;

    @BeforeEach
    void setup() {
        game = new Game(GAME_ID, PLAYERS, TURNTIME);
        game.clearField();
        initializer.initializeCards();
        when(reactiveRedisTemplateForGame.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.set(eq("game:" + GAME_ID), eq(game))).thenReturn(Mono.just(true));
        valueOperations.set("game:" + GAME_ID, game).block();
    }

    @Test
    void useActionCardSuccess() {
        //given: player1 -> player2 BROKEN_PICKAXE
        ActionCardUseRequest request = new ActionCardUseRequest(109, player1.getPlayerId(), player2.getPlayerId(), null, null, null);
        //when
        Mono<ActionCardUsedResponse> response = gameService.useActionCard(GAME_ID, request);
        //then
        StepVerifier.create(response)
                .assertNext(res -> {
                    // 1. message 검사
                    assertEquals("success", res.message());

                    // 2. gameId 검사
                    assertEquals(GAME_ID, res.gameId());

                    // 3. changedPlayerCardId 검사
                    assertEquals(request.cardId(), res.changedPlayerCardId());

                    // 4. changedTargetPlayerState 검사
                    assertTrue(player2.getState().contains(PlayerState.BROKEN_PICKAXE));

                    // 5. field 검사
                    assertNull(res.field());
                })
                .verifyComplete();
    }


}
