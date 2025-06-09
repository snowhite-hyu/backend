package com.snowhite.server.domain.session;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.snowhite.server.domain.enums.GameState;
import com.snowhite.server.domain.enums.PlayerRole;
import lombok.*;

import java.util.*;

@Data
@NoArgsConstructor
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
    private int turnTime;

    public Game(long gameId, List<Player> players, int turnTime) {
        this.gameId = gameId;
        this.players = players;
        joinedPlayerIds = new ArrayList<>();
        round = 0;
        gameState = GameState.WAITING;
        field = new Integer[7][9][4];
        deck = new ArrayList<>();
        goldCards = new ArrayList<>();
        currentTurnPlayerId = 0;
        this.turnTime = turnTime;
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

    @JsonIgnore
    public int getPlayerCount() {
        return players.size();
    }

    @JsonIgnore
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
        setGoldCards();
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
                field[i][j][1] = 0; // isRotated - 0: 카드 그대로, 1: 카드 돌아감
                field[i][j][2] = 0; // isFlipped - 0: 카드 보임, 1: 카드 안보임
                field[i][j][3] = 0; // isConnectedFromStart - 0: 카드 출발지와 연결 안됨, 1: 카드 출발지와 연결됨
            }
        }
    }

    private void clearDeck() {
        deck.clear();
    }

    public void drawAndGiveCardToPlayer(long playerId) {
        Optional<Integer> cardIdOptional = drawCard();
        cardIdOptional.ifPresent(cardId -> giveCardToPlayer(cardId, playerId));
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

    public boolean nextTurnAndReturnRoundFinished() {
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

        for (int i = 0; i < players.size(); i++) {
            int nextTurnIndex = (currentTurnPlayerIndex + 1) % players.size();
            Player nextPlayer = players.get(nextTurnIndex);
            if (!nextPlayer.getHand().isEmpty()) { // 핸드가 비어있지 않아야 턴 할당
                currentTurnPlayerId = players.get(nextTurnIndex).getPlayerId();
                return false;
            }
        }

        return true;
    }

    private boolean checkDeckAndHandsEmpty() {
        boolean handsEmpty = players.stream()
                .map(Player::getHand)
                .allMatch(List::isEmpty);

        return deck.isEmpty() && handsEmpty;
    }

    public int placeCard(int row, int column, int cardId, int isRotated, int isFlipped, int isConnectedFromStart) {
        field[row][column][0] = cardId;
        field[row][column][1] = isRotated;
        field[row][column][2] = isFlipped;
        field[row][column][3] = isConnectedFromStart;
        return cardId;
    }

    public void showCard(int row, int column) {
        field[row][column][2] = 1;
    }

    public int getCardId(int row, int column) {
        return field[row][column][0];
    }

    public void disconnectFromStart(int row, int column) {
        field[row][column][3] = 0;
    }

    public boolean isConnectedFromStart(int row, int column) {
        return field[row][column][3] == 1;
    }

    @JsonIgnore
    public int getFieldRowLength() {
        return field.length;
    }

    @JsonIgnore
    public int getFieldColumnLength() {
        return field[0].length;
    }

    public boolean isStillConnectedFromStart(int row, int column) {

        boolean stillConnected = false;
        boolean hasAdjacent = false;

        if (row > 0 && field[row - 1][column][0] != -1) {
            hasAdjacent = true;
            if (field[row - 1][column][3] == 1) {
                stillConnected = true;
            }
        }
        if (row < field.length - 1 && field[row + 1][column][0] != -1) {
            hasAdjacent = true;
            if (field[row + 1][column][3] == 1) {
                stillConnected = true;
            }
        }
        if (column > 0 && field[row][column - 1][0] != -1) {
            hasAdjacent = true;
            if (field[row][column - 1][3] == 1) {
                stillConnected = true;
            }
        }
        if (column < field[0].length - 1 && field[row][column + 1][0] != -1) {
            hasAdjacent = true;
            if (field[row][column + 1][3] == 1) {
                stillConnected = true;
            }
        }

        return hasAdjacent && stillConnected;
    }

    // Path Card 사용 후 핸드에서 제거, 금 목적지 도달 여부 리턴
    public boolean placePathCardAndReturnRoundFinished(long playerId, int cardId, int row, int column, int isRotated) {
        placeCard(row, column, cardId, isRotated, 0, 1);
        findPlayer(playerId).get().dropCard(cardId);

        int goldRow = 1;
        int goldColumn = 8;

        if (field[3][8][0] == 63) {
            goldRow = 3;
        }
        if (field[5][8][0] == 63) {
            goldRow = 5;
        }

        if ((row == goldRow - 1 && column == goldColumn)
                || (row == goldRow + 1 && column == goldColumn)
                || (row == goldRow && column == goldColumn - 1)
                || (row == goldRow && column == goldColumn + 1)
        ) {
            return true;
        }

        return false;

    }

    @JsonIgnore
    public Player getWinnerPlayer() {
        Player winner = null;
        int maxGold = -1;
        for (Player player : players) {
            if (player.getGold() > maxGold) {
                maxGold = player.getGold();
                winner = player;
            }
        }
        return winner;
    }

    // 출발지, 목적지 카드 세팅
    private void initializeNewField() {
        clearField();
        List<Integer> cardIds = new ArrayList<>();
        cardIds.add(61);
        cardIds.add(62);
        cardIds.add(63);
        Collections.shuffle(cardIds);

        placeCard(3, 0, 0, 0, 0, 1);
        placeCard(1, 8, cardIds.get(0), 0, 1, 0);
        placeCard(3, 8, cardIds.get(1), 0, 1, 0);
        placeCard(5, 8, cardIds.get(2), 0, 1, 0);
    }

    public void removeCard(int row, int col) {
        field[row][col][0] = -1;
        field[row][col][1] = 0;
        field[row][col][2] = 0;
        field[row][col][3] = 0;
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

    @JsonIgnore
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
        List<Player> saboteurPlayers = getSaboteurPlayers();
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

        for (Player saboteurPlayer : saboteurPlayers) {
            result.put(saboteurPlayer.getPlayerId(), 0);
        }

        return result;
    }

    // 큰 금덩이 카드부터 사용하면서 정해진 수만큼 분배
    public Map<Long, Integer> distributeGoldToSaboteur() {
        Map<Long, Integer> result = new HashMap<>();
        List<Player> dwarfPlayers = getDwarfPlayers();
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

        for (Player dwarfPlayer : dwarfPlayers) {
            result.put(dwarfPlayer.getPlayerId(), 0);
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

    public void refreshFieldConnectedFromStart() {
        int rowLength = getFieldRowLength();
        int columnLength = getFieldColumnLength();

        // 모든 카드의 isConnectedFromStart 초기화
        for (int r = 0; r < rowLength; r++) {
            for (int c = 0; c < columnLength; c++) {
                if (field[r][c][0] != -1) {
                    field[r][c][3] = 0; // 연결 초기화
                }
            }
        }

        // 시작 카드부터 dfs 시작
        dfsAndSetIsConnected(3, 0);
    }

    private void dfsAndSetIsConnected(int row, int column) {

        if (row < 0 || row >= field.length || column < 0 || column >= field[0].length) return; // dfs 종료 조건
        if (field[row][column][0] == -1 || field[row][column][3] == 1) return; // 카드가 없거나 이미 방문해서 연결 표시 했거나

        field[row][column][3] = 1; // 연결 표시

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // 상하좌우

        for (int[] direction : directions) {
            int adjacentRow = row + direction[0];
            int adjacentColumn = column + direction[1];

            // 범위 벗어나면 패스
            if (adjacentRow < 0 || adjacentRow >= field.length || adjacentColumn < 0 || adjacentColumn >= field[0].length) continue;
            // 카드 없으면 패스
            if (field[adjacentRow][adjacentColumn][0] == -1) continue;

            dfsAndSetIsConnected(adjacentRow, adjacentColumn);
        }
    }



    public int isFlipped(int row, int col) {
        return field[row][col][2];
    }

    public int isRotated(int row, int col) {
        return field[row][col][1];
    }

    public Integer getPathCardIdAt(Integer row, Integer col) {
        // 시작, 목적지 카드인 경우
        if ((row == 3 && col == 0) || (row == 1 && col == 8) || (row == 3 && col == 8) || (row == 5 && col == 8)) return -1;
        else return field[row][col][0];
    }

    public Integer getDestCardIdAt(Integer row, Integer col) {
        // 목적지 카드가 맞는 경우
        if ((row == 1 && col == 8) || (row == 3 && col == 8) || (row == 5 && col == 8)) return field[row][col][0];
        else return -1;
    }

}