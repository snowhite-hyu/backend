package com.snowhite.server.service;

import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.session.Game;
import com.snowhite.server.domain.session.Player;
import com.snowhite.server.websocket.dto.response.GameResponse;
import com.snowhite.server.websocket.dto.response.SecretPlayerResponse;
import com.snowhite.server.websocket.dto.response.nextround.NextRoundGameResponse;
import com.snowhite.server.websocket.dto.response.nextround.NextRoundPlayersResponse;
import com.snowhite.server.websocket.dto.response.nextround.NextRoundResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final String GAME_PREFIX = "game:";
    private static final String CARD_PREFIX = "card:";

    private final ReactiveRedisTemplate<String, Game> reactiveRedisTemplateForGame;
    private final ReactiveRedisTemplate<String, Card> reactiveRedisTemplateForCard;

    // game에 player를 join시킨 후 남은 player 수 리턴
    public Mono<Integer> joinPlayer(Long gameId, Long playerId) {

        return getGameByGameId(gameId)
                .map(game -> {
                    int remain = game.joinPlayerAndReturnRemain(playerId);
                    setGameToRedis(gameId, game);
                    return remain;
                });
    }

    // 해당 game에 모든 player가 join했는지 확인
    public Mono<Boolean> verifyAllJoined(Long gameId) {
        return getGameByGameId(gameId)
                .map(game -> game.getJoinedPlayerIds().size() == game.getPlayers().size());
    }

    // 새로운 round 시작 또는 round 종료
    public Mono<NextRoundResponse> processNextRoundOrFinishRound(Long gameId) {

        return getGameByGameId(gameId)
                .flatMap(game -> {
                    boolean isFinished = game.startNextRoundAndReturnGameFinished();
                    if (isFinished) {
                        return setGameToRedis(gameId, game)
                                .then(getAllSecretPlayerInfo(game))
                                .map(NextRoundPlayersResponse::of);
                    } else {
                        return setGameToRedis(gameId, game)
                                .thenReturn(NextRoundGameResponse.of(GameResponse.from(game)));
                    }
                });
    }

    public Mono<List<SecretPlayerResponse>> getAllSecretPlayerInfo(Game game) {

        return Mono.just(
                game.getPlayers().stream()
                        .map(SecretPlayerResponse::from)
                        .toList()
        );
    }

    public Mono<Game> getGameByGameId(Long gameId) {
        return reactiveRedisTemplateForGame.opsForValue().get(GAME_PREFIX + gameId);
    }

    public Mono<Boolean> setGameToRedis(Long gameId, Game game) {
        return reactiveRedisTemplateForGame.opsForValue().set(GAME_PREFIX + gameId, game);
    }

    public Mono<Player> findPlayerByGameIdAndPlayerId(Long gameId, Long playerId) {
        return getGameByGameId(gameId)
                .map(game -> game.findPlayer(playerId).get());
    }

    // 게임에 필요한 카드 정보 가져오기
    private Flux<Card> getAllCardsFromRedis() {
        return reactiveRedisTemplateForCard
                .scan(ScanOptions.scanOptions().match(CARD_PREFIX).build())
                .flatMap(key -> reactiveRedisTemplateForCard.opsForValue().get(key));
    }

}
