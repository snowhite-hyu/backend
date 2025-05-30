package com.snowhite.server.service;

import com.snowhite.server.domain.session.Game;
import com.snowhite.server.domain.session.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class GameServiceTest {

    private GameService gameService;
    private ReactiveRedisTemplate<String, Game> gameRedisTemplate;
    private ReactiveValueOperations<String, Game> valueOperations;

    @BeforeEach
    void setUp() {
        gameRedisTemplate = Mockito.mock(ReactiveRedisTemplate.class);
        valueOperations = Mockito.mock(ReactiveValueOperations.class);
        when(gameRedisTemplate.opsForValue()).thenReturn(valueOperations);

        gameService = new GameService(gameRedisTemplate, null);
    }

    @Test
    void dropCard_successfully_removes_card() {
        // given
        Long gameId = 1L;
        Long playerId = 100L;
        int cardId = 42;
        String gameKey = "game:" + gameId;

        Player player = new Player(playerId, "테스터");
        player.getCards().add(cardId);

        List<Player> players = new ArrayList<>();
        players.add(player);

        Game game = new Game(gameId, players, 30);

        when(valueOperations.get(eq(gameKey))).thenReturn(Mono.just(game));
        when(valueOperations.set(eq(gameKey), any(Game.class))).thenReturn(Mono.just(true));

        // when
        Mono<Player> result = gameService.dropCard(gameId, playerId, cardId);

        // then
        StepVerifier.create(result)
                .expectNextMatches(updatedPlayer -> !updatedPlayer.getCards().contains(cardId))
                .verifyComplete();
    }

    @Test
    void dropCard_fails_when_player_not_found() {
        Long gameId = 1L;
        Long playerId = 100L;
        int cardId = 42;
        String gameKey = "game:" + gameId;

        Game game = new Game(gameId, new ArrayList<>(), 30);

        when(valueOperations.get(eq(gameKey))).thenReturn(Mono.just(game));

        Mono<Player> result = gameService.dropCard(gameId, playerId, cardId);

        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().equals("플레이어가 없음"))
                .verify();
    }

    @Test
    void dropCard_fails_when_card_not_in_hand() {
        Long gameId = 1L;
        Long playerId = 100L;
        int cardId = 42;
        String gameKey = "game:" + gameId;

        Player player = new Player(playerId, "테스터");

        Game game = new Game(gameId, List.of(player), 30);

        when(valueOperations.get(eq(gameKey))).thenReturn(Mono.just(game));

        Mono<Player> result = gameService.dropCard(gameId, playerId, cardId);

        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().equals("해당 카드가 없음"))
                .verify();
    }

    @Test
    void dropCard_fails_when_game_not_found() {
        Long gameId = 1L;
        Long playerId = 100L;
        int cardId = 42;
        String gameKey = "game:" + gameId;

        when(valueOperations.get(eq(gameKey))).thenReturn(Mono.empty());

        Mono<Player> result = gameService.dropCard(gameId, playerId, cardId);

        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().equals("게임이 없음"))
                .verify();
    }
}