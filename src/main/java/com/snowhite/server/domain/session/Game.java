package com.snowhite.server.domain.session;

import com.snowhite.server.domain.enums.GameState;
import com.snowhite.server.domain.enums.PlayerRole;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
public class Game {

    private long gameId;
    private List<Player> players;
    private List<Long> joinedPlayerIds;
    private int round;
    private GameState gameState;
    private Integer[][][] field;
    private List<Integer> deck;
    private long currentTurnPlayerId;
    private int turnTime;   // second

    public Game(long gameId, List<Player> players, int turnTime) {
        this.gameId = gameId;
        this.players = players;
        this.joinedPlayerIds = new ArrayList<>();
        this.round = 0;
        this.gameState = GameState.WAITING;
        field = new Integer[7][9][2];
        deck = new ArrayList<>();
        currentTurnPlayerId = 0;
        this.turnTime = turnTime;
    }

    public void clearFieldAndDeck() {
        this.field = new Integer[7][9][2];
        deck = new ArrayList<>();
    }

    public int incrementRoundAndChangeGameState() {
        round++;
        if (round == 1) {
            this.gameState = GameState.IN_GAME;
        } else if (round == 4) {
            this.gameState = GameState.FINISHED;
        }
        return round;
    }

    public Optional<Integer> drawCard() {
        if (deck.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(deck.removeFirst());
    }

    public void giveCardToPlayer(int cardId, long playerId) {
        findPlayer(playerId).get().addCard(cardId);
    }

    public void drawAndGiveCardToPlayer(long playerId) {
        int cardId = drawCard().get();
        giveCardToPlayer(cardId, playerId);
    }

    public void distributeRoles(int dwarf, int saboteur) {
        List<PlayerRole> roles = new ArrayList<>();
        for (int i = 0; i < dwarf; i++) {
            roles.add(PlayerRole.DWARF);
        }
        for (int i = 0; i < saboteur; i++) {
            roles.add(PlayerRole.SABOTEUR);
        }
        Collections.shuffle(roles);
        for (int i = 0; i < players.size(); i++) {
            players.get(i).changePlayerRole(roles.get(i));
        }
    }

    public void shufflePlayers() {
        Collections.shuffle(players);
    }

    public long nextTurn() {
        int currentTurnPlayerIndex = 0;
        if (currentTurnPlayerId != 0) {
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).getPlayerId() == currentTurnPlayerId) {
                    currentTurnPlayerIndex = i;
                    break;
                }
            }
        }
        int nextTurnIndex = (currentTurnPlayerIndex + 1) % players.size();
        currentTurnPlayerId = players.get(nextTurnIndex).getPlayerId();
        return currentTurnPlayerId;
    }

    public int placeCard(int row, int column, int cardId, int isFlipped) {
        this.field[row][column][0] = cardId;
        this.field[row][column][1] = isFlipped;
        return cardId;
    }

    public void addCardsToDeck(List<Integer> cardIds) {
        this.deck.addAll(cardIds);
    }

    public void joinPlayer(Long playerId) {
        this.joinedPlayerIds.add(playerId);
    }

    public Optional<Player> findPlayer(long playerId) {
        return this.players.stream()
                .filter(player -> player.getPlayerId() == playerId)
                .findFirst();
    }

    public int getPlayerCount() {
        return this.players.size();
    }



}
