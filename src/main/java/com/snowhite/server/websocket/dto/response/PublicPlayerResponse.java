package com.snowhite.server.websocket.dto.response;

import com.snowhite.server.domain.enums.PlayerState;
import com.snowhite.server.domain.session.Player;

public record PublicPlayerResponse(
        long playerId,
        String playerName,
        int handSize,
        PlayerState state,
        int gold
) {
    public static PublicPlayerResponse from(Player player) {
        return new PublicPlayerResponse(
                player.getPlayerId(),
                player.getPlayerName(),
                player.getHand().size(),
                player.getState(),
                player.getGold()
        );
    }
}
