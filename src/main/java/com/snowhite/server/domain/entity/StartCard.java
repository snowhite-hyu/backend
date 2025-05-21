package com.snowhite.server.domain.entity;

import com.snowhite.server.domain.enums.CardType;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Entity
@NoArgsConstructor
public class StartCard extends Card{
    private List<Boolean> path;
    public StartCard(Integer id, String name, List<Boolean> path) {
        super(id, name, CardType.START);
        this.path = path;
    }
}
