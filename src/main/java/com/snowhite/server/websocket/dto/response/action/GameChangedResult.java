package com.snowhite.server.websocket.dto.response.action;

public record GameChangedResult(
        Integer[][][] field
) {
    public static GameChangedResult of(Integer[][][] field){
        return new GameChangedResult(field);
    }
}
