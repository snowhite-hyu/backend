package com.snowhite.server.init;

import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.enums.CardType;
import com.snowhite.server.repository.CardRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CardInitializer {

    private static final String CARD_PREFIX = "card:";

    private final CardRepository cardRepository;
    private final ReactiveRedisTemplate<String, Card> reactiveRedisTemplateForCard;

    private final EntityManager entityManager;

    // 게임 로직에 필요한 카드 세팅
    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initializeCards() {

        List<Card> cardList = List.of(
                new Card(0, "start", CardType.START),
                new Card(1, "cave1", CardType.CAVE, true, true, true, false, true),
                new Card(2, "cave2", CardType.CAVE, true, true, true, false, true),
                new Card(3, "cave3", CardType.CAVE, true, true, true, false, true),
                new Card(4, "cave4", CardType.CAVE, true, true, true, false, true),
                new Card(5, "cave5", CardType.CAVE, true, true, true, false, true),
                new Card(6, "cave6", CardType.CAVE, true, true, false, false, true),
                new Card(7, "cave7", CardType.CAVE, true, true, false, false, true),
                new Card(8, "cave8", CardType.CAVE, true, true, false, false, true),
                new Card(9, "cave9", CardType.CAVE, true, true, false, false, true),
                new Card(10, "cave10", CardType.CAVE, false, true, false, true, true),
                new Card(11, "cave11", CardType.CAVE, false, true, false, true, true),
                new Card(12, "cave12", CardType.CAVE, false, true, false, true, true),
                new Card(13, "cave13", CardType.CAVE, false, true, false, true, true),
                new Card(14, "cave14", CardType.CAVE, false, true, true, false, true),
                new Card(15, "cave15", CardType.CAVE, false, true, true, false, true),
                new Card(16, "cave16", CardType.CAVE, false, true, true, false, true),
                new Card(17, "cave17", CardType.CAVE, false, true, true, false, true),
                new Card(18, "cave18", CardType.CAVE, false, true, true, false, true),
                new Card(19, "cave19", CardType.CAVE, false, true, true, true, true),
                new Card(20, "cave20", CardType.CAVE, false, true, true, true, true),
                new Card(21, "cave21", CardType.CAVE, false, true, true, true, true),
                new Card(22, "cave22", CardType.CAVE, false, true, true, true, true),
                new Card(23, "cave23", CardType.CAVE, false, true, true, true, true),
                new Card(24, "cave24", CardType.CAVE, false, false, true, false, true),
                new Card(25, "cave25", CardType.CAVE, false, false, true, false, true),
                new Card(26, "cave26", CardType.CAVE, false, false, true, false, true),
                new Card(27, "cave27", CardType.CAVE, true, true, true, true, true),
                new Card(28, "cave28", CardType.CAVE, true, true, true, true, true),
                new Card(29, "cave29", CardType.CAVE, true, true, true, true, true),
                new Card(30, "cave30", CardType.CAVE, true, true, true, true, true),
                new Card(31, "cave31", CardType.CAVE, true, true, true, true, true),
                new Card(32, "cave32", CardType.CAVE, true, true, true, false, false),
                new Card(33, "cave33", CardType.CAVE, false, true, false, false, true),
                new Card(34, "cave34", CardType.CAVE, false, false, true, false, true),
                new Card(35, "cave35", CardType.CAVE, false, true, false, true, false),
                new Card(36, "cave36", CardType.CAVE, true, true, true, false, false),
                new Card(37, "cave37", CardType.CAVE, false, true, true, true, false),
                new Card(38, "cave38", CardType.CAVE, true, true, true, true, false),
                new Card(39, "cave39", CardType.CAVE, true, true, false, false, false),
                new Card(40, "cave40", CardType.CAVE, false, false, true, true, false),
                new Card(61, "destination1", CardType.DESTINATION),
                new Card(62, "destination2", CardType.DESTINATION),
                new Card(63, "destination3", CardType.DESTINATION),
                new Card(101, "tool - pickaxe", CardType.ACTION),
                new Card(102, "tool - lantern", CardType.ACTION),
                new Card(103, "tool - minecart", CardType.ACTION),
                new Card(104, "tool - pickaxe, lantern", CardType.ACTION),
                new Card(105, "tool - pickaxe, minecart", CardType.ACTION),
                new Card(106, "tool - lantern, minecart", CardType.ACTION),
                new Card(107, "rockfall", CardType.ACTION),
                new Card(108, "map", CardType.ACTION),
                new Card(109, "broken - pickaxe", CardType.ACTION),
                new Card(110, "broken - lantern", CardType.ACTION),
                new Card(111, "broken - minecart", CardType.ACTION)
        );

        cardRepository.deleteAllInBatch();
        cardRepository.saveAll(cardList);
        cardRepository.findAll().forEach(card -> {
            reactiveRedisTemplateForCard.opsForValue()
                    .set(CARD_PREFIX + card.getId(), card)
                    .block();
        });

    }
}
