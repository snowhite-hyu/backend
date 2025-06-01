package com.snowhite.server.domain.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.snowhite.server.domain.enums.GameState;
import com.snowhite.server.domain.enums.PlayerRole;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private boolean hasPathFromStart;

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
        hasPathFromStart = false; // rockfall로 인해 끊김을 체크
    }

    public Game() {}

    public void clearFieldAndDeck() {
        clearField();
        clearDeck();
    }

    public void clearField() {
        this.field = new Integer[7][9][2];
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 9; j++) {
                field[i][j][0] = -1;    // -1: 카드 x
                field[i][j][1] = 0; // isflipped
            }
        }
    }


    private void clearDeck() {
        this.deck = new ArrayList<>();
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

    public void removeCard(int row, int col) {
        this.field[row][col] = null;
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

    public void setNewDeck() {
        clearDeck();
        for (int i = 0; i <= 40; i++) {
            deck.add(i);    // 굴
        }
        for (int i = 0; i < 2; i++) {
            deck.add(101);  // 곡괭이 수리
            deck.add(102);  // 랜턴 수리
            deck.add(103);  // 수레 수리
        }
        deck.add(104);  // 곡괭이, 랜턴 수리
        deck.add(105);  // 곡괭이, 수레 수리
        deck.add(106);  // 랜턴, 수레 수리
        for (int i = 0; i < 3; i++) {
            deck.add(107);  // 낙석
            deck.add(109);  // 곡괭이 파손
            deck.add(110);  // 랜턴 파손
            deck.add(111);  // 수레 파손
        }
        for (int i = 0; i < 6; i++) {
            deck.add(108);  // 지도
        }
    }
    // TODO: field 확장 기능 추가 후 구현
    public boolean isPossibleLocationToGetCard(int row, int col) {
        return true;
    }

    public boolean isFlipped(int row, int col) {
        if (field[row][col][1] == 1) { return true; }
        else { return false; }
    }

    public Integer getCardIdAt(Integer row, Integer col) {
        return field[row][col][0];
    }

}