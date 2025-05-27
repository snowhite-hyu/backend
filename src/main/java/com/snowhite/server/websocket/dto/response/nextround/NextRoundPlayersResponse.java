package com.snowhite.server.websocket.dto.response.nextround;

import com.snowhite.server.websocket.dto.response.SecretPlayerResponse;

import java.util.List;

public record NextRoundPlayersResponse(
        List<SecretPlayerResponse> players
) implements NextRonudResponse{
    public static NextRoundPlayersResponse of(List<SecretPlayerResponse> players) {
        return new NextRoundPlayersResponse(players);
    }
}
