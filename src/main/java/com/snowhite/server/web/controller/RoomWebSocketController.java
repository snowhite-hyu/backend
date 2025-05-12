package com.snowhite.server.web.controller;

import com.snowhite.server.service.RoomWebSocketService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RoomWebSocketController {

    private final RoomWebSocketService roomWebSocketService;

    public RoomWebSocketController(RoomWebSocketService roomWebSocketService) {
        this.roomWebSocketService = roomWebSocketService;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    @Bean
    public HandlerMapping webSocketMapping() {
        Map<String, WebSocketHandler> map = new ConcurrentHashMap<>();
        map.put("/rooms", roomWebSocketService);

        return new SimpleUrlHandlerMapping() {{
            setOrder(10);
            setUrlMap(map);
        }};
    }
}