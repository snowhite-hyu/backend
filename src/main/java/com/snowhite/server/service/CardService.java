package com.snowhite.server.service;

import com.snowhite.server.domain.entity.Card;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CardService {

    private static final String CARD_PREFIX = "card:";

    private final ReactiveRedisTemplate<String, Card> reactiveRedisTemplateForCard;

    public Mono<Card> findCardByCardId(int cardId) {
        return reactiveRedisTemplateForCard.opsForValue().get(CARD_PREFIX + cardId);
    }
}
