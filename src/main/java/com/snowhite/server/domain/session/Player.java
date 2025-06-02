package com.snowhite.server.domain.session;

import com.snowhite.server.domain.enums.PlayerRole;
import com.snowhite.server.domain.enums.PlayerState;
import lombok.Getter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Getter
public class Player {
    private final long playerId;
    private final String playerName;
    private PlayerRole playerRole;
    private final List<Integer> hand;
    private EnumSet<PlayerState> state;
    private int gold;

    public Player(long playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        hand = new ArrayList<>();
        state = EnumSet.of(PlayerState.NORMAL);
        gold = 0;
    }

    public void addCardToHand(int cardId) {
        hand.add(cardId);
    }

    public boolean dropCard(int cardId) { return hand.remove((Integer) cardId); }

    public void changePlayerRole(PlayerRole playerRole) {
        this.playerRole = playerRole;
    }

    public void clearHand() {
        hand.clear();
    }

    public void initializePlayerStateToNormal() {
        state = EnumSet.of(PlayerState.NORMAL);
    }

    public void addPlayerState(PlayerState state) {
        if (!hasState(state)) { this.state.add(state); }
    }

    public void removePlayerState(PlayerState state) {
        if (hasState(state)) { this.state.remove(state); }
    }

    public boolean hasState(PlayerState state) {
        return this.state.contains(state);
    }

    public int addGold(int gold) {
        this.gold += gold;
        return this.gold;
    }

    public void removeCard(int cardId) {
        this.hand.remove(cardId);
    }

    public boolean hasCard(Integer cardId) { return this.hand.contains(cardId); }

}