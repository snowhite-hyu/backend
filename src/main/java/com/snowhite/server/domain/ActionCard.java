package com.snowhite.server.domain;

public class ActionCard extends Card{
    private final CardType cardType = CardType.ACTION;
    private ActionCardType actionCardType;

    public ActionCardType getActionCardType() { return this.actionCardType; }

}
