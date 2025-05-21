package com.snowhite.server.websocket.dto.request;

import lombok.Getter;

@Getter
public class ActionCardUseRequest {
    private Integer cardId;
    private Long playerId;
    private Long targetPlayerId;
    private Integer locationX;
    private Integer locationY;
}
