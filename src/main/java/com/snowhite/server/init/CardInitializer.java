package com.snowhite.server.init;

import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.repository.CardRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class CardInitializer {

    private final CardRepository cardRepository;
    private final ReactiveRedisTemplate<Long, Card> reactiveRedisTemplateForCard;

    private final EntityManager entityManager;

    // 게임 로직에 필요한 카드 세팅
    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initializeCards() {

        entityManager.createQuery("DELETE FROM Card").executeUpdate();

        // TODO: 모든 카드 INSERT

        Flux.fromIterable(cardRepository.findAll())
                .flatMap(card -> reactiveRedisTemplateForCard.opsForValue().set(card.getId(), card))
                .then()
                .block();
        }

}
