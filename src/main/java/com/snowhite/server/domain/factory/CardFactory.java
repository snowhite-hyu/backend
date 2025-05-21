package com.snowhite.server.domain.factory;

import com.snowhite.server.domain.entity.*;
import com.snowhite.server.domain.enums.ActionCardType;
import com.snowhite.server.domain.enums.CardType;

import java.util.List;
import java.util.Map;

public class CardFactory {
    private CardFactory() {} // 인스턴스화 금지
   public static Card createPathCard(
           Integer id,
           String name,
           List<Boolean> path,
           Boolean isSpin
   ) { return new PathCard(id, name, path, isSpin); }

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
            List<Boolean> path
    ) {
        return new StartCard(id, name, path);
    }

    public static Card createDestinationCard(
            Integer id,
            String name,
            List<Boolean> path,
            Boolean isSpin,
            Boolean isGold
    ) {
        return new DestinationCard(id, name, path, isSpin, isGold);
    }

}
