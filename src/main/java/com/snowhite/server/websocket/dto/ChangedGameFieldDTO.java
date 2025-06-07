package com.snowhite.server.websocket.dto;

public record ChangedGameFieldDTO(
        Integer[][][] field
) {
    public static ChangedGameFieldDTO of(Integer[][][] field){
        return new ChangedGameFieldDTO(field);
    }
}
