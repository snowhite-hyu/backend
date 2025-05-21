package com.snowhite.server.domain.entity;

import com.snowhite.server.domain.enums.CardType;
import lombok.Getter;

import java.util.List;

@Getter
public class StartCard extends Card{
    private List<Boolean> path;
    public StartCard(Integer id, String name, List<Boolean> path) {
        super(id, name, CardType.START);
        this.path = path;
    }
}
