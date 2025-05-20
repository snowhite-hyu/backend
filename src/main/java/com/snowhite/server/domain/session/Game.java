package com.snowhite.server.domain.session;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Game {

    private long gameId;
    private List<Player> players;
    private List<Long> joinedPlayerIds;
    private Integer[][] field;
    private List<Integer> deck;
    private long currentTurnPlayerId;
    private int turnTime;   // second

    public Game(long gameId, List<Player> players) {
        this.gameId = gameId;
        this.players = players;
        this.joinedPlayerIds = new ArrayList<>();
        field = new Integer[7][9];
        deck = new ArrayList<>();
        currentTurnPlayerId = 0;
        turnTime = 0;
    }

    public void joinPlayer(Long playerId) {
        this.joinedPlayerIds.add(playerId);
    }

}