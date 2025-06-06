package com.snowhite.server.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PlayerState {
    NORMAL("NORMAL"),
    BROKEN_PICKAXE("BROKEN_PICKAXE"),
    BROKEN_MINECART("BROKEN_MINECART"),
    BROKEN_LANTERN("BROKEN_LANTERN");
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
