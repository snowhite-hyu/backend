package com.snowhite.server.domain.entity;

import com.snowhite.server.domain.enums.CardType;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Entity
@NoArgsConstructor
public class DestinationCard extends Card{
    private boolean up_open;
    private boolean down_open;
    private boolean left_open;
    private boolean right_open;
    private boolean middle_open;
    private boolean is_spin;
    private boolean is_gold;
    public DestinationCard(Integer id, String name, Boolean up_open, Boolean down_open, Boolean left_open, Boolean right_open, Boolean middle_open, Boolean is_spin, Boolean is_gold) {
        super(id, name, CardType.DESTINATION);
        this.up_open = up_open;
        this.down_open = down_open;
        this.left_open = left_open;
        this.right_open = right_open;
        this.middle_open = middle_open;
        this.is_spin = is_spin;
        this.is_gold = is_gold;
    }

    public void setIs_spin(boolean is_spin) {
        this.is_spin = is_spin;
    }
}
