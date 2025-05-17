package com.snowhite.server.domain.session;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Game {

    private long gameId;
    private List<Player> players;
    private Integer[][] field;
    private List<Integer> deck;
    private long currentTurnPlayerId;
    private int turnTime;   // second

    public Game(long gameId, List<Player> players) {
        this.gameId = gameId;
        this.players = players;
        field = new Integer[7][9];
        deck = new ArrayList<>();
        currentTurnPlayerId = 0;
        turnTime = 0;
    }
}