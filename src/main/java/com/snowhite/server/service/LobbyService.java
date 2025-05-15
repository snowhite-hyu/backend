package com.snowhite.server.service;

import com.snowhite.server.domain.Room;
import com.snowhite.server.payload.ApiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
public class LobbyService {

    @Qualifier("reactiveRedisTemplateForRooms")
    private final ReactiveRedisTemplate<String, Room> redisTemplateForRooms;

    public Flux<String> scanRoomKeys() {
        ScanOptions options =
                ScanOptions.scanOptions().match("room:*").count(1000).build();
        return redisTemplateForRooms.scan(options);
    }

    public LobbyService(
            ReactiveRedisTemplate<String, Room> redisTemplateForRooms
    ) {
        this.redisTemplateForRooms = redisTemplateForRooms;
    }

    public Mono<ServerResponse> getRooms(ServerRequest request) {

        return scanRoomKeys()
                .flatMap(roomId -> redisTemplateForRooms.opsForValue().get(roomId))
                .collectList()
                .flatMap(rooms ->
                {
                    if (rooms == null) {
                        rooms = Collections.emptyList();
                    }

                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(ApiResponse.onSuccess(rooms));
                });
    }
}