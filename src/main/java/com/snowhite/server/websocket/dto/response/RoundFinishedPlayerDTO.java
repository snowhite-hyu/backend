package com.snowhite.server.websocket.dto.response;

import com.snowhite.server.domain.enums.PlayerRole;

public record RoundFinishedPlayerDTO(
        Long playerId,
        String playerName,
        PlayerRole role,
        Integer gainedGold
) {
    public static RoundFinishedPlayerDTO of(Long playerId, String playerName, PlayerRole role, Integer gainedGold) {
        return new RoundFinishedPlayerDTO(playerId, playerName, role, gainedGold);
    }
}
