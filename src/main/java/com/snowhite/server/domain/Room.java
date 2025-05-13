package com.snowhite.server.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
public class Room {

    private Long roomId;
    private User masterPlayer;
    private List<User> users;
    private int capacity;
    private int turnTime;
    private boolean isPlaying;

    public Room(Long roomId, User masterPlayer, List<User> users, int capacity, int turnTime, boolean isPlaying) {
        this.roomId = roomId;
        this.masterPlayer = masterPlayer;
        this.users = users;
        this.capacity = capacity;
        this.turnTime = turnTime;
        this.isPlaying = isPlaying;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return roomId.equals(room.roomId) &&
                capacity == room.capacity &&
                turnTime == room.turnTime &&
                isPlaying == room.isPlaying;
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, capacity, turnTime, isPlaying);
    }
}
