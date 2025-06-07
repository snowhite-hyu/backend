package com.snowhite.server.domain.entity;

import com.snowhite.server.domain.enums.CardType;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Entity
@NoArgsConstructor
public class PathCard extends Card{
    private boolean up_open;
    private boolean down_open;
    private boolean left_open;
    private boolean right_open;
    private boolean middle_open;

    public PathCard(Integer id, String name, Boolean up_open, Boolean down_open, Boolean left_open, Boolean right_open, Boolean middle_open){
        super(id, name, CardType.PATH);
        this.up_open = up_open;
        this.down_open = down_open;
        this.left_open = left_open;
        this.right_open = right_open;
        this.middle_open = middle_open;
    }

    public boolean isUpperOpened(int flipped) {
        if (flipped == 1) {
            return down_open;
        }
        return up_open;
    }

    public boolean isLowerOpened(int flipped) {
        if (flipped == 1) {
            return up_open;
        }
        return down_open;
    }

    public boolean isLeftOpened(int flipped) {
        if (flipped == 1) {
            return right_open;
        }
        return left_open;
    }

    public boolean isRightOpened(int flipped) {
        if (flipped == 1) {
           return left_open;
        }
        return right_open;
    }

}
