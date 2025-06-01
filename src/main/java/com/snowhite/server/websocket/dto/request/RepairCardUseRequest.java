package com.snowhite.server.websocket.dto.request;

import com.snowhite.server.domain.enums.PlayerState;

public record RepairCardUseRequest(
        Long gameId,
        Long playerId,
        Long targetPlayerId,
        Integer cardId,
        PlayerState targetState
) {
}
