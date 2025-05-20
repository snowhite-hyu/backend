package com.snowhite.server.domain.entity;

import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.enums.ActionCardType;
import com.snowhite.server.domain.enums.CardType;

public class ActionCard extends Card {
    private final CardType cardType = CardType.ACTION;
    private ActionCardType actionCardType;

    public ActionCardType getActionCardType() { return this.actionCardType; }

}
