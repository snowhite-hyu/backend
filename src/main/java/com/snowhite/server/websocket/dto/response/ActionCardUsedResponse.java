package com.snowhite.server.websocket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.snowhite.server.domain.enums.PlayerState;

import java.util.List;

public record ActionCardUsedResponse (
        String message,
        Long gameId,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer changedPlayerCardId,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<PlayerState> changedTargetPlayerState,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer[][][] field
) {
    public static ActionCardUsedResponse of(String message, Long gameId, Integer changedPlayerCardId, List<PlayerState> changedTargetPlayerState, Integer[][][] field) {
        return new ActionCardUsedResponse(message, gameId, changedPlayerCardId, changedTargetPlayerState, field);
    }

    public ActionCardUsedResponse getSingleResponse(){
        return new ActionCardUsedResponse(message, gameId, changedPlayerCardId, null, null);
    }

    public ActionCardUsedResponse getBroadCastResponse() {
        return new ActionCardUsedResponse(message, gameId, null, changedTargetPlayerState, field);
    }
}
