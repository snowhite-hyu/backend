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
    private List<Boolean> path;
    private boolean is_spin;
    private boolean is_gold;
    public DestinationCard(Integer id, String name, List<Boolean> path, Boolean is_spin, Boolean is_gold) {
        super(id, name, CardType.DESTINATION);
        this.path = path;
        this.is_spin = is_spin;
        this.is_gold = is_gold;
    }

    public void setIs_spin(boolean is_spin) {
        this.is_spin = is_spin;
    }
}
