package com.snowhite.server.domain.session;

import com.fasterxml.jackson.annotation.JsonView;
import com.snowhite.server.domain.enums.ActionCardType;
import com.snowhite.server.domain.enums.PlayerRole;
import com.snowhite.server.domain.enums.PlayerState;
import com.snowhite.server.websocket.dto.response.View;
import lombok.Getter;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Getter
public class Player {
    @JsonView({View.Unicast.class, View.Broadcast.class})
    private final long playerId;

    @JsonView(View.Broadcast.class)
    private final String playerName;

    @JsonView(View.Unicast.class)
    private PlayerRole playerRole;

    @JsonView(View.Unicast.class)
    private final List<Integer> cards;

    @JsonView(View.Broadcast.class)
    private EnumSet<PlayerState> state;

    @JsonView(View.Broadcast.class)
    private int gold;

    public Player(long playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        cards = new ArrayList<>();
        state = EnumSet.of(PlayerState.NORMAL);
        gold = 0;
    }

    public void addCard(int cardId) {
        cards.add(cardId);
    }

    public void changePlayerRole(PlayerRole playerRole) {
        this.playerRole = playerRole;
    }

    public void clearHand() {
        cards.clear();
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
        this.cards.remove(cardId);
    }
    public boolean hasCard(Integer cardId) { return this.cards.contains(cardId); }
}