package com.snowhite.server.websocket.dto.response;

import com.snowhite.server.domain.enums.PlayerRole;

import java.util.List;

public record RoundFinishedResponse(
        PlayerRole winnerRole,
        List<RoundFinishedPlayerDTO> players
) {
    public static RoundFinishedResponse of(PlayerRole winnerRole, List<RoundFinishedPlayerDTO> players) {
        return new RoundFinishedResponse(winnerRole, players);
    }
}
