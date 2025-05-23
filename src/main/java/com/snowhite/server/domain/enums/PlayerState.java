package com.snowhite.server.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PlayerState {
    NORMAL("normal"),
    BROKEN_PICKAXE("broken_pickaxe"),
    BROKEN_MINCART("broken_mincart"),
    BROKEN_LANTERN("broken_lantern");
    private final String description;

    @JsonCreator
    public static PlayerState from(String description) {
        for(PlayerState state : PlayerState.values()) {
            if (state.getDescription().equals(description)) {
                return state;
            }
        }
        throw new IllegalArgumentException("unknown state: " + description);
    }
}
