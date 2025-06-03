package com.snowhite.server.domain.session;

import com.snowhite.server.domain.enums.GameState;
import com.snowhite.server.domain.enums.PlayerRole;
import lombok.Getter;

import java.util.*;

@Getter
public class Game {

    private long gameId;
    private List<Player> players;
    private List<Long> joinedPlayerIds;
    private int round;
    private GameState gameState;
    private Integer[][][] field;
    private List<Integer> deck;
    private List<Integer> goldCards;
    private long currentTurnPlayerId;
    private int turnTime;   // second
    private boolean hasPathFromStart;

    public Game(long gameId, List<Player> players, int turnTime) {
        this.gameId = gameId;
        this.players = players;
        joinedPlayerIds = new ArrayList<>();
        round = 0;
        gameState = GameState.WAITING;
        field = new Integer[7][9][2];
        deck = new ArrayList<>();
        goldCards = new ArrayList<>();
        currentTurnPlayerId = 0;
        this.turnTime = turnTime;
        hasPathFromStart = false; // rockfall로 인해 끊김을 체크
    }

    public int joinPlayerAndReturnRemain(Long playerId) {
        joinedPlayerIds.add(playerId);
        return players.size() - joinedPlayerIds.size();
    }

    public Optional<Player> findPlayer(long playerId) {
        return players.stream()
                .filter(player -> player.getPlayerId() == playerId)
                .findFirst();
    }

    public int getPlayerCount() {
        return players.size();
    }

    public int getDeckSize() {
        return deck.size();
    }

    public boolean startNextRoundAndReturnGameFinished() {
        round++;
        if (round >= 1 && round <= 3) {
            gameState = GameState.IN_GAME;
        }
        if (round >= 4) {
            gameState = GameState.FINISHED;
            return true;
        }

        setNewDeck();
        shufflePlayers();
        initializeAllPlayersState();
        initializeNewField();
        initializeAllPlayersRole();
        initializeAllPlayersHands();
        nextTurnAndReturnRoundFinished();
        return false;
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
        deck.clear();
    }

    public void drawAndGiveCardToPlayer(long playerId) {
        int cardId = drawCard().get();
        giveCardToPlayer(cardId, playerId);
    }

    public Optional<Integer> drawCard() {
        if (deck.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(deck.removeFirst());
    }

    private void giveCardToPlayer(int cardId, long playerId) {
        findPlayer(playerId).get().addCardToHand(cardId);
    }

    private void distributeRoles(int dwarf, int saboteur) {
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

    private void shufflePlayers() {
        Collections.shuffle(players);
    }

    private void shuffleDeck() {
        Collections.shuffle(deck);
    }

    private void shuffleGoldCards() {
        Collections.shuffle(goldCards);
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

    private boolean nextTurnAndReturnRoundFinished() {
        if (checkDeckAndHandsEmpty()) {
            return true;
        }
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
        return false;
    }

    private boolean checkDeckAndHandsEmpty() {
        boolean handsEmpty = players.stream()
                .map(Player::getHand)
                .allMatch(List::isEmpty);

        return deck.isEmpty() && handsEmpty;
    }

    public int placeCard(int row, int column, int cardId, int isFlipped) {
        field[row][column][0] = cardId;
        field[row][column][1] = isFlipped;
        return cardId;
    }

    // 출발지, 목적지 카드 세팅
    private void initializeNewField() {
        clearField();
        List<Integer> cardIds = new ArrayList<>();
        cardIds.add(61);
        cardIds.add(62);
        cardIds.add(63);
        Collections.shuffle(cardIds);

        placeCard(3, 0, 0, 0);
        placeCard(1, 8, cardIds.get(0), 0);
        placeCard(3, 8, cardIds.get(1), 0);
        placeCard(5, 8, cardIds.get(2), 0);
    }

    public void removeCard(int row, int col) {
        this.field[row][col] = null;
    }

    public void addCardsToDeck(List<Integer> cardIds) {
        this.deck.addAll(cardIds);
    }

    // 모든 player의 state 초기화
    private void initializeAllPlayersState() {
        players.forEach(Player::initializePlayerStateToNormal);
    }

    // 모든 player의 역할 초기화
    private void initializeAllPlayersRole() {
        int playerCount = getPlayerCount();
        int dwarf = 0;
        int saboteur = 0;

        switch (playerCount) {
            case 3:
                dwarf = 3;
                saboteur = 1;
                break;
            case 4:
                dwarf = 4;
                saboteur = 1;
                break;
            case 5:
                dwarf = 4;
                saboteur = 2;
                break;
            case 6:
                dwarf = 5;
                saboteur = 2;
                break;
            case 7:
                dwarf = 5;
                saboteur = 3;
                break;
            case 8:
                dwarf = 6;
                saboteur = 3;
                break;
            case 9:
                dwarf = 7;
                saboteur = 3;
                break;
            case 10:
                dwarf = 7;
                saboteur = 4;
                break;
            default:
                break;
        }
        distributeRoles(dwarf, saboteur);
    }

    // 모든 player의 손패 초기화
    private void initializeAllPlayersHands() {
        int playerCount = getPlayerCount();
        int cardNumber = 0;

        switch(playerCount) {
            case 3, 4, 5:
                cardNumber = 6;
                break;
            case 6, 7:
                cardNumber = 5;
                break;
            case 8, 9, 10:
                cardNumber = 4;
                break;
            default:
                break;
        }

        for (Player player : players) {
            player.clearHand();
            for (int i = 0; i < cardNumber; i++) {
                drawAndGiveCardToPlayer(player.getPlayerId());
            }
        }
    }

    public void setNewDeck() {
        clearDeck();
        for (int i = 1; i <= 40; i++) {
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

        shuffleDeck();
    }

    private void setGoldCards() {
        goldCards.clear();
        for (int i = 0; i < 16; i++) {
            goldCards.add(1);
        }
        for (int i = 0; i < 8; i++) {
            goldCards.add(2);
        }
        for (int i = 0; i < 4; i++) {
            goldCards.add(3);
        }
        shuffleGoldCards();
    }

    // winner는 가장 큰 금덩이 카드를, 나머지는 무작위로 분배
    public Map<Long, Integer> distributeGoldToDwarf(long winnerPlayerId) {
        Map<Long, Integer> result = new HashMap<>();
        List<Player> dwarfPlayers = getDwarfPlayers();
        List<Integer> goldCardsToDistribute = new ArrayList<>();

        for (int i = 0; i < dwarfPlayers.size(); i++) {
            goldCardsToDistribute.add(goldCards.removeFirst());
        }

        goldCardsToDistribute.sort(Collections.reverseOrder());
        int maxGold = goldCardsToDistribute.removeFirst();
        Collections.shuffle(goldCardsToDistribute);

        for (Player dwarfPlayer : dwarfPlayers) {
            if (dwarfPlayer.getPlayerId() == winnerPlayerId) {
                dwarfPlayer.addGold(maxGold);
                result.put(winnerPlayerId, maxGold);
            } else {
                int goldToGive = goldCardsToDistribute.removeFirst();
                dwarfPlayer.addGold(goldToGive);
                result.put(dwarfPlayer.getPlayerId(), goldToGive);
            }
        }

        return result;
    }

    // 큰 금덩이 카드부터 사용하면서 정해진 수만큼 분배
    public Map<Long, Integer> distributeGoldToSaboteur() {
        Map<Long, Integer> result = new HashMap<>();
        List<Player> saboteurPlayers = getSaboteurPlayers();

        int saboteurCount = saboteurPlayers.size();
        if (saboteurCount == 0) {
            return result;
        }

        int goldPerSaboteur;
        if (saboteurCount == 1) {
            goldPerSaboteur = 4;
        } else if (saboteurCount == 2 || saboteurCount == 3) {
            goldPerSaboteur = 3;
        } else {    // saboteurCount == 4
            goldPerSaboteur = 2;
        }

        goldCards.sort(Collections.reverseOrder());
        for (Player saboteurPlayer : saboteurPlayers) {
            int givenGold = 0;
            Iterator<Integer> iterator = goldCards.iterator();
            while (iterator.hasNext()) {
                int gold = iterator.next();
                if (givenGold + gold <= goldPerSaboteur) {
                    givenGold += gold;
                    iterator.remove();
                }
            }

            saboteurPlayer.addGold(givenGold);
            result.put(saboteurPlayer.getPlayerId(), givenGold);
        }

        shuffleGoldCards();

        return result;
    }

    private List<Player> getDwarfPlayers() {
        return players.stream()
                .filter(player -> player.getPlayerRole() == PlayerRole.DWARF)
                .toList();
    }

    private List<Player> getSaboteurPlayers() {
        return players.stream()
                .filter(player -> player.getPlayerRole() == PlayerRole.SABOTEUR)
                .toList();
    }
    // TODO: field 확장 기능 추가 후 구현
    public boolean isPossibleLocationToGetCard(int row, int col) {
        return true;
    }

    public boolean isFlipped(int row, int col) {
        if (field[row][col][1] == 1) { return true; }
        else { return false; }
    }

}