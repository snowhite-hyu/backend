package com.snowhite.server.service;

import com.snowhite.server.domain.session.Game;
import com.snowhite.server.domain.session.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class GameServiceTest {

    private GameService gameService;
    private ReactiveRedisTemplate<String, Game> gameRedisTemplate;
    private ReactiveValueOperations<String, Game> valueOperations;

    @BeforeEach
    void setUp() {
        gameRedisTemplate = mock(ReactiveRedisTemplate.class);
        valueOperations = mock(ReactiveValueOperations.class);
        when(gameRedisTemplate.opsForValue()).thenReturn(valueOperations);

        gameService = new GameService(gameRedisTemplate, null);
    }

    @Test
    void getCard_successfullyDrawsCardAndSavesGame() {
        Long gameId = 1L;
        Long playerId = 10L;
        String gameKey = "game:" + gameId;

        Game mockGame = mock(Game.class);
        Player mockPlayer = mock(Player.class);
        Integer drawnCard = 5;

        List<Integer> playerCards = new ArrayList<>();

        when(gameRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(gameKey)).thenReturn(Mono.just(mockGame));
        when(mockGame.findPlayer(playerId)).thenReturn(Optional.of(mockPlayer));
        when(mockGame.drawCard()).thenReturn(Optional.of(drawnCard));
        when(mockPlayer.getCards()).thenReturn(playerCards);
        when(valueOperations.set(gameKey, mockGame)).thenReturn(Mono.just(true));

        StepVerifier.create(gameService.getCard(gameId, playerId))
                .verifyComplete();

        assertEquals(1, playerCards.size());
        assertEquals(drawnCard, playerCards.get(0));
        verify(valueOperations).set(gameKey, mockGame);
    }

    @Test
    void getCard_noCardLeft_shouldThrowError() {
        Long gameId = 1L;
        Long playerId = 10L;
        String gameKey = "game:" + gameId;

        Game mockGame = mock(Game.class);
        Player mockPlayer = mock(Player.class);

        when(gameRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(gameKey)).thenReturn(Mono.just(mockGame));
        when(mockGame.findPlayer(playerId)).thenReturn(Optional.of(mockPlayer));
        when(mockGame.drawCard()).thenReturn(Optional.empty());

        StepVerifier.create(gameService.getCard(gameId, playerId))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("남아있는 카드가 없음"))
                .verify();
    }
}
