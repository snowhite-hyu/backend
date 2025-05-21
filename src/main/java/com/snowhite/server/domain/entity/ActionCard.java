package com.snowhite.server.domain.entity;

import com.snowhite.server.domain.enums.ActionCardType;
import com.snowhite.server.domain.enums.CardType;
import lombok.Getter;

@Getter
public class ActionCard extends Card {
    private ActionCardType actionCardType;

    public ActionCard(Integer id, String name, ActionCardType actionCardType) {
        super(id, name, CardType.ACTION);
        this.actionCardType = actionCardType;
    }
}
