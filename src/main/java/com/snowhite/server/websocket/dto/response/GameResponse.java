package com.snowhite.server.websocket.dto.response;

import com.snowhite.server.domain.enums.GameState;
import com.snowhite.server.domain.session.Game;

import java.util.List;

public record GameResponse(
        long gameId,
        List<PublicPlayerResponse> players,
        List<Long> joinedPlayerIds,
        int round,
        GameState gameState,
        Integer[][][] field,
        int deckSize,
        long currentTurnPlayerId,
        int turnTime
) {
    public static GameResponse from(Game game) {
        return new GameResponse(
                game.getGameId(),
                game.getPlayers()
                        .stream()
                        .map(PublicPlayerResponse::from)
                        .toList(),
                game.getJoinedPlayerIds(),
                game.getRound(),
                game.getGameState(),
                game.getField(),
                game.getDeckSize(),
                game.getCurrentTurnPlayerId(),
                game.getTurnTime()
        );
    }

}
