package com.snowhite.server.domain.entity;

import com.snowhite.server.domain.enums.ActionCardType;
import com.snowhite.server.domain.enums.CardType;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
public class ActionCard extends Card {
    private ActionCardType actionCardType;

    public ActionCard(Integer id, String name, ActionCardType actionCardType) {
        super(id, name, CardType.ACTION);
        this.actionCardType = actionCardType;
    }
}
