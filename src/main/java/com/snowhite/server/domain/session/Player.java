package com.snowhite.server.domain.session;

import com.snowhite.server.domain.enums.PlayerState;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Player {

    private final long playerId;
    private final String playerName;
    private final List<Integer> cards;
    private PlayerState state;
    private int gold;

    public Player(long playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        cards = new ArrayList<>();
        state = PlayerState.NORMAL;
        gold = 0;
    }

    public void addCard(int card) {
        cards.add(card);
    }

    public void updatePlayerState(PlayerState state) {
        this.state = state;
    }

    public int addGold(int gold) {
        this.gold += gold;
        return this.gold;
    }
}
