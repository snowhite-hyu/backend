package com.snowhite.server.domain.session;

import com.snowhite.server.domain.enums.PlayerRole;
import com.snowhite.server.domain.enums.PlayerState;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Data
@NoArgsConstructor
public class Player {
    private long playerId;
    private String playerName;
    private PlayerRole playerRole;
    private List<Integer> hand;
    private EnumSet<PlayerState> state;
    private int gold;

    public Player(long playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        hand = new ArrayList<>();
        this.state = EnumSet.of(PlayerState.NORMAL);
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
        if (!hasState(state)) {
            this.state.add(state);
        }
        if(state != PlayerState.NORMAL && hasState(PlayerState.NORMAL)) {
            removePlayerState(PlayerState.NORMAL);
        }
    }

    public void removePlayerState(PlayerState state) {
        if (hasState(state)) {
            this.state.remove(state);
        }
        if (this.state.isEmpty()) {
            addPlayerState(PlayerState.NORMAL);
        }
    }

    public boolean hasState(PlayerState state) {
        return this.state.contains(state);
    }

    public int addGold(int gold) {
        this.gold += gold;
        return this.gold;
    }

    public void removeCard(int cardId) {
        this.hand.remove(Integer.valueOf(cardId));
    }

    public boolean hasCard(Integer cardId) { return this.hand.contains(cardId); }

    public int getHandSize() { return this.hand.size(); }
}