package com.snowhite.server.init;

import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.enums.ActionCardType;
import com.snowhite.server.domain.factory.CardFactory;
import com.snowhite.server.repository.CardRepository;
import lombok.RequiredArgsConstructor;
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

    private final CardRepository cardRepository;
    private final ReactiveRedisTemplate<String, Card> reactiveRedisTemplateForCard;

    // 게임 로직에 필요한 카드 세팅
    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initializeCards() {
        List<Card> cardList = List.of(
                CardFactory.createStartCard(0, "start", List.of(true, true, true, true, true)),
                CardFactory.createPathCard(1, "path1", List.of(true, true, true, false, true), true),
                CardFactory.createPathCard(2, "path2", List.of(true, true, true, false, true), true),
                CardFactory.createPathCard(3, "path3", List.of(true, true, true, false, true), true),
                CardFactory.createPathCard(4, "path4", List.of(true, true, true, false, true), true),
                CardFactory.createPathCard(5, "path5", List.of(true, true, true, false, true), true),
                CardFactory.createPathCard(6, "path6", List.of(true, true, false, false, true), true),
                CardFactory.createPathCard(7, "path7", List.of(true, true, false, false, true), true),
                CardFactory.createPathCard(8, "path8", List.of(true, true, false, false, true), true),
                CardFactory.createPathCard(9, "path9", List.of(true, true, false, false, true), true),
                CardFactory.createPathCard(10, "path10", List.of(false, true, false, true, true), true),
                CardFactory.createPathCard(11, "path11", List.of(false, true, false, true, true), true),
                CardFactory.createPathCard(12, "path12", List.of(false, true, false, true, true), true),
                CardFactory.createPathCard(13, "path13", List.of(false, true, false, true, true), true),
                CardFactory.createPathCard(14, "path14", List.of(false, true, true, false, true), true),
                CardFactory.createPathCard(15, "path15", List.of(false, true, true, false, true), true),
                CardFactory.createPathCard(16, "path16", List.of(false, true, true, false, true), true),
                CardFactory.createPathCard(17, "path17", List.of(false, true, true, false, true), true),
                CardFactory.createPathCard(18, "path18", List.of(false, true, true, false, true), true),
                CardFactory.createPathCard(19, "path19", List.of(false, true, true, true, true), true),
                CardFactory.createPathCard(20, "path20", List.of(false, true, true, true, true), true),
                CardFactory.createPathCard(21, "path21", List.of(false, true, true, true, true), true),
                CardFactory.createPathCard(22, "path22", List.of(false, true, true, true, true), true),
                CardFactory.createPathCard(23, "path23", List.of(false, true, true, true, true), true),
                CardFactory.createPathCard(24, "path24", List.of(false, false, true, false, true), true),
                CardFactory.createPathCard(25, "path25", List.of(false, false, true, false, true), true),
                CardFactory.createPathCard(26, "path26", List.of(false, false, true, false, true), true),
                CardFactory.createPathCard(27, "path27", List.of(true, true, true, true, true), true),
                CardFactory.createPathCard(28, "path28", List.of(true, true, true, true, true), true),
                CardFactory.createPathCard(29, "path29", List.of(true, true, true, true, true), true),
                CardFactory.createPathCard(30, "path30", List.of(true, true, true, true, true), true),
                CardFactory.createPathCard(31, "path31", List.of(true, true, true, true, true), true),
                CardFactory.createPathCard(32, "path32", List.of(true, true, true, false, false), true),
                CardFactory.createPathCard(33, "path33", List.of(false, true, false, false, true), true),
                CardFactory.createPathCard(34, "path34", List.of(false, false, true, false, true), true),
                CardFactory.createPathCard(35, "path35", List.of(false, true, false, true, false), true),
                CardFactory.createPathCard(36, "path36", List.of(true, true, true, false, false), true),
                CardFactory.createPathCard(37, "path37", List.of(false, true, true, true, false), true),
                CardFactory.createPathCard(38, "path38", List.of(true, true, true, true, false), true),
                CardFactory.createPathCard(39, "path39", List.of(true, true, false, false, false), true),
                CardFactory.createPathCard(40, "path40", List.of(false, false, true, true, false), true),
                CardFactory.createDestinationCard(61, "destination1", List.of(false, false, false, false, false), false, true),
                CardFactory.createDestinationCard(62, "destination2", List.of(false, false, false, false, false), false, true),
                CardFactory.createDestinationCard(63, "destination3", List.of(false, false, false, false, false), false, true),
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