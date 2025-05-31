package com.snowhite.server.websocket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.snowhite.server.domain.enums.PlayerState;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActionCardUsedResponse(
        // 공통 필드
        Long gameId,
        String message,
        Integer actionCardId,
        // 유니캐스트 응답
        Long usePlayerId,
        List<Integer> usePlayerCards,
        // 브로드캐스트 응답
        Long targetPlayerId,
        List<PlayerState> targetPlayerState,
        Integer[][][] field
) {
        public static ActionCardUsedResponse ofUnicast(Long gameId, String message, Integer actionCardId, Long usePlayerId, List<Integer> usePlayerCards) {
                return new ActionCardUsedResponse(
                        gameId,
                        message,
                        actionCardId,
                        usePlayerId,
                        usePlayerCards,
                        null,
                        null,
                        null
                );
        }
        public static ActionCardUsedResponse ofBroadcast(Long gameId, String message, Integer actionCardId, Long targetPlayerId, List<PlayerState> targetPlayerState, Integer[][][] field) {
                return new ActionCardUsedResponse(
                        gameId,
                        message,
                        actionCardId,
                        null,
                        null,
                        targetPlayerId,
                        targetPlayerState,
                        field
                );
        }
        public static class Builder {
                private Long gameId;
                private String message;
                private Integer actionCardId;
                private Long usePlayerId;
                private List<Integer> usePlayerCards;
                private Long targetPlayerId;
                private List<PlayerState> targetPlayerState;
                private Integer[][][] field;

                public Builder message(String message) {
                        this.message = message;
                        return this;
                }

                public Builder gameId(Long gameId) {
                        this.gameId = gameId;
                        return this;
                }

                public Builder actionCardId(Integer actionCardId) {
                        this.actionCardId = actionCardId;
                        return this;
                }

                public Builder usePlayerId(Long usePlayerId) {
                        this.usePlayerId = usePlayerId;
                        return this;
                }

                public Builder usePlayerCards(List<Integer> usePlayerCards) {
                        this.usePlayerCards = usePlayerCards;
                        return this;
                }

                public Builder targetPlayerId(Long targetPlayerId) {
                        this.targetPlayerId = targetPlayerId;
                        return this;
                }

                public Builder targetPlayerState(List<PlayerState> targetPlayerState) {
                        this.targetPlayerState = targetPlayerState;
                        return this;
                }

                public Builder field(Integer[][][] field) {
                        this.field = field;
                        return this;
                }

                public ActionCardUsedResponse build() {
                        return new ActionCardUsedResponse(
                                gameId,
                                message,
                                actionCardId,
                                usePlayerId,
                                usePlayerCards,
                                targetPlayerId,
                                targetPlayerState,
                                field
                        );
                }
        }
}
