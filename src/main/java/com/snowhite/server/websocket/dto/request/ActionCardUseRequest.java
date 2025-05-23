package com.snowhite.server.websocket.dto.request;

import com.snowhite.server.domain.enums.PlayerState;
import jakarta.validation.constraints.NotNull;

public record ActionCardUseRequest (
        @NotNull Long gameId,
        @NotNull int cardId,
        @NotNull Long playerId,

        Long targetPlayerId,
        Integer locationX,
        Integer locationY,
        PlayerState targetRepairState
) {

}
