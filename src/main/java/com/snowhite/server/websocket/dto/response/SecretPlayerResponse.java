package com.snowhite.server.websocket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.snowhite.server.domain.enums.PlayerRole;
import com.snowhite.server.domain.enums.PlayerState;
import com.snowhite.server.domain.session.Player;

import java.util.EnumSet;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SecretPlayerResponse(
        long playerId,
        String playerName,
        PlayerRole playerRole,
        List<Integer> hand,
        EnumSet<PlayerState> state,
        Integer gold
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
