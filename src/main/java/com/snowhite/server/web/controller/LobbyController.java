package com.snowhite.server.web.controller;

import com.snowhite.server.service.LobbyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class LobbyController {

    @Bean
    public RouterFunction<ServerResponse> lobbyRouter(LobbyService service) {
        return RouterFunctions.route()
                .GET("/lobby", RequestPredicates.accept(MediaType.APPLICATION_JSON), service::getRooms)
                .build();
    }

}
