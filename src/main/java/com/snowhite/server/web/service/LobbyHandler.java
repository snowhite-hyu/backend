package com.snowhite.server.web.service;

import com.snowhite.server.web.domain.Room;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class LobbyHandler {

    private final ReactiveRedisTemplate<String, Long> redisTemplateForIds;
    private final ReactiveRedisTemplate<Long, Room> redisTemplateForRooms;

    public LobbyHandler(
            ReactiveRedisTemplate<String, Long> redisTemplateForIds,
            ReactiveRedisTemplate<Long, Room> redisTemplateForRooms
    ) {
        this.redisTemplateForIds = redisTemplateForIds;
        this.redisTemplateForRooms = redisTemplateForRooms;
    }

    public Mono<ServerResponse> getRooms(ServerRequest request) {

        return redisTemplateForIds.opsForSet().members("rooms")
                .flatMap(roomId -> redisTemplateForRooms.opsForValue().get(roomId))
                .collectList()
                .flatMap(rooms -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(rooms));
    }
}