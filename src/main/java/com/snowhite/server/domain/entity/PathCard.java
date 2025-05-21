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
    private List<Boolean> path;
    private boolean is_spin;
    public PathCard(Integer id, String name, List<Boolean> path, Boolean is_spin){
        super(id, name, CardType.PATH);
        this.path = path;
        this.is_spin = is_spin;
    }

}
