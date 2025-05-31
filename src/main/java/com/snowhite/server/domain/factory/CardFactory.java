package com.snowhite.server.domain.factory;

import com.snowhite.server.domain.entity.*;
import com.snowhite.server.domain.enums.ActionCardType;

public class CardFactory {
    private CardFactory() {} // 인스턴스화 금지
   public static Card createPathCard(
           Integer id,
           String name,
           Boolean up_open,
           Boolean down_open,
           Boolean left_open,
           Boolean right_open,
           Boolean middle_open
   ) { return new PathCard(id, name, up_open, down_open, left_open, right_open, middle_open); }

    public static Card createActionCard(
            Integer id,
            String name,
            ActionCardType actionType
    ) {
        return new ActionCard(id, name, actionType);
    }

    public static Card createStartCard(
            Integer id,
            String name,
            Boolean up_open,
            Boolean down_open,
            Boolean left_open,
            Boolean right_open,
            Boolean middle_open
    ) {
        return new StartCard(id, name, up_open, down_open, left_open, right_open, middle_open);
    }

    public static Card createDestinationCard(
            Integer id,
            String name,
            Boolean up_open,
            Boolean down_open,
            Boolean left_open,
            Boolean right_open,
            Boolean middle_open,
            Boolean isGold
    ) {
        return new DestinationCard(id, name, up_open, down_open, left_open, right_open, middle_open, isGold);
    }

}
