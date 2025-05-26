package com.snowhite.server.init;

import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.enums.ActionCardType;
import com.snowhite.server.domain.factory.CardFactory;
import com.snowhite.server.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CardInitializer {

    private static final String CARD_PREFIX = "card:";

    @Autowired
    private final CardRepository cardRepository;
    private final ReactiveRedisTemplate<String, Card> reactiveRedisTemplateForCard;
    private final Boolean SPIN = true;

    // 게임 로직에 필요한 카드 세팅
    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initializeCards() {
        List<Card> cardList = List.of(
                CardFactory.createStartCard(0, "start", true, true, true, true, true),
                CardFactory.createPathCard(1, "path1", true, true, true, false, true),
                CardFactory.createPathCard(2, "path2", true, true, true, false, true),
                CardFactory.createPathCard(3, "path3", true, true, true, false, true),
                CardFactory.createPathCard(4, "path4", true, true, true, false, true),
                CardFactory.createPathCard(5, "path5", true, true, true, false, true),
                CardFactory.createPathCard(6, "path6", true, true, false, false, true),
                CardFactory.createPathCard(7, "path7", true, true, false, false, true),
                CardFactory.createPathCard(8, "path8", true, true, false, false, true),
                CardFactory.createPathCard(9, "path9", true, true, false, false, true),
                CardFactory.createPathCard(10, "path10", false, true, false, true, true),
                CardFactory.createPathCard(11, "path11", false, true, false, true, true),
                CardFactory.createPathCard(12, "path12", false, true, false, true, true),
                CardFactory.createPathCard(13, "path13", false, true, false, true, true),
                CardFactory.createPathCard(14, "path14", false, true, true, false, true),
                CardFactory.createPathCard(15, "path15", false, true, true, false, true),
                CardFactory.createPathCard(16, "path16", false, true, true, false, true),
                CardFactory.createPathCard(17, "path17", false, true, true, false, true),
                CardFactory.createPathCard(18, "path18", false, true, true, false, true),
                CardFactory.createPathCard(19, "path19", false, true, true, true, true),
                CardFactory.createPathCard(20, "path20", false, true, true, true, true),
                CardFactory.createPathCard(21, "path21", false, true, true, true, true),
                CardFactory.createPathCard(22, "path22", false, true, true, true, true),
                CardFactory.createPathCard(23, "path23", false, true, true, true, true),
                CardFactory.createPathCard(24, "path24", false, false, true, false, true),
                CardFactory.createPathCard(25, "path25", false, false, true, false, true),
                CardFactory.createPathCard(26, "path26", false, false, true, false, true),
                CardFactory.createPathCard(27, "path27", true, true, true, true, true),
                CardFactory.createPathCard(28, "path28", true, true, true, true, true),
                CardFactory.createPathCard(29, "path29", true, true, true, true, true),
                CardFactory.createPathCard(30, "path30", true, true, true, true, true),
                CardFactory.createPathCard(31, "path31", true, true, true, true, true),
                CardFactory.createPathCard(32, "path32", true, true, true, false, false),
                CardFactory.createPathCard(33, "path33", false, true, false, false, true),
                CardFactory.createPathCard(34, "path34", false, false, true, false, true),
                CardFactory.createPathCard(35, "path35", false, true, false, true, false),
                CardFactory.createPathCard(36, "path36", true, true, true, false, false),
                CardFactory.createPathCard(37, "path37", false, true, true, true, false),
                CardFactory.createPathCard(38, "path38", true, true, true, true, false),
                CardFactory.createPathCard(39, "path39", true, true, false, false, false),
                CardFactory.createPathCard(40, "path40", false, false, true, true, false),
                CardFactory.createDestinationCard(61, "destination1", false, false, false, false, false, true),
                CardFactory.createDestinationCard(62, "destination2", false, false, false, false, false, false),
                CardFactory.createDestinationCard(63, "destination3", false, false, false, false, false, false),
                CardFactory.createActionCard(101, "tool - pickaxe", ActionCardType.REPAIR_PICKAXE),
                CardFactory.createActionCard(102, "tool - lantern", ActionCardType.REPAIR_LANTERN),
                CardFactory.createActionCard(103, "tool - minecart", ActionCardType.REPAIR_MINECART),
                CardFactory.createActionCard(104, "tool - pickaxe, lantern", ActionCardType.REPAIR_PICKAXE_AND_LANTERN),
                CardFactory.createActionCard(105, "tool - pickaxe, minecart", ActionCardType.REPAIR_PICKAXE_AND_MINECART),
                CardFactory.createActionCard(106, "tool - lantern, minecart", ActionCardType.REPAIR_LANTERN_MINECART),
                CardFactory.createActionCard(107, "rockfall", ActionCardType.ROCKFALL),
                CardFactory.createActionCard(108, "map", ActionCardType.MAP),
                CardFactory.createActionCard(109, "broken - pickaxe", ActionCardType.BROKEN_PICKAXE),
                CardFactory.createActionCard(110, "broken - lantern", ActionCardType.BROKEN_LANTERN),
                CardFactory.createActionCard(111, "broken - minecart", ActionCardType.BROKEN_MINECART)
        );
        cardRepository.deleteAll();
        cardRepository.flush();
        cardRepository.saveAll(cardList);
        cardRepository.findAll().forEach(card -> {
            reactiveRedisTemplateForCard.opsForValue()
                    .set(CARD_PREFIX + card.getId(), card)
                    .block();
        });

    }
}