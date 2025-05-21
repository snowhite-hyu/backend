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
    private boolean is_spin;
    public PathCard(Integer id, String name, Boolean up_open, Boolean down_open, Boolean left_open, Boolean right_open, Boolean middle_open, Boolean is_spin){
        super(id, name, CardType.PATH);
        this.up_open = up_open;
        this.down_open = down_open;
        this.left_open = left_open;
        this.right_open = right_open;
        this.middle_open = middle_open;
        this.is_spin = is_spin;
    }

}
