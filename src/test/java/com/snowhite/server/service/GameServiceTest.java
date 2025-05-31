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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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
    void getCard_successfully() {
        Long gameId = 1L;
        Long playerId = 10L;
        String gameKey = "game:" + gameId;

        Player player = new Player(playerId, "테스터");
        List<Player> players = List.of(player);
        Game game = new Game(gameId, players, 60);

        when(valueOperations.get(eq(gameKey))).thenReturn(Mono.just(game));
        when(valueOperations.set(eq(gameKey), any(Game.class))).thenReturn(Mono.just(true));

        // 카드 얻기 전 플레이어의 카드 수
        game.setNewDeck();
        int cardSize = player.getCards().size();

        // 카드 한장 가져오기
        Player updatedPlayer = gameService.getCard(gameId, playerId).block();

        // 카드 얻기 전 카드 수 + 1 된건지 확인
        List<Integer> cards = updatedPlayer.getCards();
        assertEquals(cardSize+1, cards.size());

        // 가져온 카드가 더미카드에 없는지 확인
        List<Integer> deck = game.getDeck();
        assertFalse(deck.contains(cards.get(cardSize)));
    }

    @Test
    void getCard_noCardLeft_shouldThrowError() {
        Long gameId = 1L;
        Long playerId = 10L;
        String gameKey = "game:" + gameId;

        Player player = new Player(playerId, "테스터");
        List<Player> players = List.of(player);
        Game game = new Game(gameId, players, 60);

        when(valueOperations.get(eq(gameKey))).thenReturn(Mono.just(game));
        when(valueOperations.set(eq(gameKey), any(Game.class))).thenReturn(Mono.just(true));

        // 카드 가져오고 에러 났는지 확인
        Mono<Player> result = gameService.getCard(gameId, playerId);
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                        e.getMessage().equals("남아있는 카드가 없음"))
                .verify();
    }
}
