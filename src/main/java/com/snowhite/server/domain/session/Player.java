package com.snowhite.server.domain.session;

import com.snowhite.server.domain.enums.PlayerRole;
import com.snowhite.server.domain.enums.PlayerState;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Player {

    private final long playerId;
    private final String playerName;
    private PlayerRole playerRole;
    private final List<Integer> hand;
    private PlayerState state;
    private int gold;

    public Player(long playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        hand = new ArrayList<>();
        state = PlayerState.NORMAL;
        gold = 0;
    }

    public void addCardToHand(int cardId) {
        hand.add(cardId);
    }

    public void changePlayerRole(PlayerRole playerRole) {
        this.playerRole = playerRole;
    }

    public void clearHand() {
        hand.clear();
    }

    public void updatePlayerState(PlayerState state) {
        this.state = state;
    }

    public int addGold(int gold) {
        this.gold += gold;
        return this.gold;
    }
}
