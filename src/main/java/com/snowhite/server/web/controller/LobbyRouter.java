package com.snowhite.server.web.controller;

import com.snowhite.server.web.service.LobbyHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

// RouterFunction 구현
@Configuration
public class LobbyRouter {

    @Bean
    public RouterFunction<ServerResponse> lobbyRouter(LobbyHandler handler) {
        return RouterFunctions.route()
                .GET("/rooms", RequestPredicates.accept(MediaType.APPLICATION_JSON), handler::getRooms)
                .build();
    }
}
