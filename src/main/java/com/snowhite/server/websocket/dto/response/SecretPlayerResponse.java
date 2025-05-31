package com.snowhite.server.websocket.dto.response;

import com.snowhite.server.domain.enums.PlayerRole;
import com.snowhite.server.domain.enums.PlayerState;
import com.snowhite.server.domain.session.Player;

import java.util.List;

public record SecretPlayerResponse(
        long playerId,
        String playerName,
        PlayerRole playerRole,
        List<Integer> cards,
        PlayerState state,
        int gold
) {
    public static SecretPlayerResponse from(Player player) {
        return new SecretPlayerResponse(
                player.getPlayerId(),
                player.getPlayerName(),
                player.getPlayerRole(),
                player.getHand(),
                player.getState(),
                player.getGold()
        );
    }
}
