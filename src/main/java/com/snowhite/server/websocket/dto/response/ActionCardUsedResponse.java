package com.snowhite.server.websocket.dto.response;

import com.fasterxml.jackson.annotation.JsonView;
import com.snowhite.server.domain.enums.PlayerState;

import java.util.List;

public record ActionCardUsedResponse(
        // 공통 필드
        @JsonView({View.Unicast.class, View.Broadcast.class, View.Error.class})
        Long gameId,
        @JsonView({View.Unicast.class, View.Broadcast.class, View.Error.class})
        String message,
        @JsonView({View.Unicast.class, View.Broadcast.class, View.Error.class})
        Integer actionCardId,

        // 카드 사용 불가 응답
        @JsonView(View.Error.class)
        Long errorUsePlayerId,

        @JsonView(View.Error.class)
        Long errorTargetPlayerId,

        @JsonView(View.Error.class)
        Integer errorLocationX,

        @JsonView(View.Error.class)
        Integer errorLocationY,

        // 유니캐스트 응답

        @JsonView(View.Unicast.class)
        Long usePlayerId,

        @JsonView(View.Unicast.class)
        List<Integer> usePlayerCards,

        @JsonView(View.Unicast.class)
        Integer destCardId,

        // 브로드캐스트 응답
        @JsonView(View.Broadcast.class)
        Long targetPlayerId,

        @JsonView(View.Broadcast.class)
        List<PlayerState> targetPlayerState,

        @JsonView(View.Broadcast.class)
        Integer[][][] field
) {
        public static class Builder {
                private Long gameId;
                private String message;
                private Integer actionCardId;
                private Long errorUsePlayerId;
                private Long errorTargetPlayerId;
                private Integer errorLocationX;
                private Integer errorLocationY;
                private Long usePlayerId;
                private List<Integer> usePlayerCards;
                private Integer destCardId;
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

                public Builder errorUsePlayerId(Long errorUsePlayerId) {
                        this.errorUsePlayerId = errorUsePlayerId;
                        return this;
                }

                public Builder errorTargetPlayerId(Long errorTargetPlayerId) {
                        this.errorTargetPlayerId = errorTargetPlayerId;
                        return this;
                }

                public Builder errorLocationX(Integer errorLocationX) {
                        this.errorLocationX = errorLocationX;
                        return this;
                }

                public Builder errorLocationY(Integer errorLocationY) {
                        this.errorLocationY = errorLocationY;
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

                public Builder destCardId(Integer destCardId) {
                        this.destCardId = destCardId;
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
                                errorUsePlayerId,
                                errorTargetPlayerId,
                                errorLocationX,
                                errorLocationY,
                                usePlayerId,
                                usePlayerCards,
                                destCardId,
                                targetPlayerId,
                                targetPlayerState,
                                field
                        );
                }
        }
}
