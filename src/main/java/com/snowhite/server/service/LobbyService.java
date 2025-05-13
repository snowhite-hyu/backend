package com.snowhite.server.service;

import com.snowhite.server.domain.Room;
import com.snowhite.server.payload.ApiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class LobbyService {

    @Qualifier("reactiveRedisTemplateForIds")
    private final ReactiveRedisTemplate<String, Long> redisTemplateForIds;

    @Qualifier("reactiveRedisTemplateForRooms")
    private final ReactiveRedisTemplate<Long, Room> redisTemplateForRooms;

    public LobbyService(
            ReactiveRedisTemplate<String, Long> redisTemplateForIds,
            ReactiveRedisTemplate<Long, Room> redisTemplateForRooms
    ) {
        this.redisTemplateForIds = redisTemplateForIds;
        this.redisTemplateForRooms = redisTemplateForRooms;
    }

    public Mono<ServerResponse> getRooms(ServerRequest request) {

        return redisTemplateForIds.opsForSet()
                .members("rooms")
                .flatMap(roomId -> redisTemplateForRooms.opsForValue().get(roomId))
                .collectList()
                .flatMap(rooms -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ApiResponse.onSuccess(rooms)));
    }
}