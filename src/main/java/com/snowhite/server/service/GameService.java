package com.snowhite.server.service;

import com.snowhite.server.domain.session.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final String GAME_PREFIX = "game:";

    private final ReactiveRedisTemplate<String, Game> reactiveRedisTemplateForGame;

    // game에 player를 join시킨 후 남은 player 수 리턴
    public Mono<Integer> joinPlayer(Long gameId, Long playerId) {

        return reactiveRedisTemplateForGame.opsForValue().get(GAME_PREFIX + gameId)
                .flatMap(game -> {
                    game.getJoinedPlayerIds().add(playerId);
                    int remain = game.getPlayers().size() - game.getJoinedPlayerIds().size();
                    return Mono.just(remain);
                });
    }

    // 해당 game에 모든 player가 join했는지 확인
    public Mono<Boolean> verifyAllJoined(Long gameId, Long playerId) {

        return reactiveRedisTemplateForGame.opsForValue().get(GAME_PREFIX + gameId)
                .flatMap(game -> {
                    game.getJoinedPlayerIds().add(playerId);
                    if (game.getJoinedPlayerIds().size() == game.getPlayers().size()) {
                        return Mono.just(true);
                    } else {
                        return Mono.just(false);
                    }
                });

    }
}
