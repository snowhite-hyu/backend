package com.snowhite.server.websocket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.snowhite.server.domain.enums.PlayerState;

import java.util.List;

public record ActionCardUsedResponse (
        String message,
        Long gameId,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer changedPlayerCard,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<PlayerState> changedTargetPlayerState,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer changedFieldX,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer changedFieldY
) {
    // 카드 사용 x
    public static ActionCardUsedResponse of(String message, Long gameId) {
        return new ActionCardUsedResponse(message, gameId, null,null, null, null);
    }
    // map
    public static ActionCardUsedResponse of(String message, Long gameId, Integer changedPlayerCard) {
        return new ActionCardUsedResponse(message, gameId, changedPlayerCard, null, null, null);
    }
    // repair, broken
    public static ActionCardUsedResponse of(String message, Long gameId, Integer changedPlayerCard, List<PlayerState> changedTargetPlayerState) {
        return new ActionCardUsedResponse(message, gameId, changedPlayerCard, changedTargetPlayerState, null, null);
    }
    // rockfall
    public static ActionCardUsedResponse of(String message, Long gameId, Integer changedPlayerCard, Integer changedFieldX, Integer changedFieldY) {
        return new ActionCardUsedResponse(message, gameId, changedPlayerCard, null, changedFieldX, changedFieldY);
    }
}
