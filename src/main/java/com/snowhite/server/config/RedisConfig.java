package com.snowhite.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snowhite.server.domain.entity.Card;
import com.snowhite.server.domain.session.Game;
import com.snowhite.server.domain.session.Room;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

@Configuration
@EnableAutoConfiguration(exclude={RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class})
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public ReactiveRedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    @Bean(name="reactiveRedisTemplateForRooms")
    public ReactiveRedisTemplate<String, Room> reactiveRedisTemplateForRooms(
            ReactiveRedisConnectionFactory factory,
            ObjectMapper objectMapper) {

        Jackson2JsonRedisSerializer<Room> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Room.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Room> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, Room> context = builder
                .value(serializer)
                .hashValue(serializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean(name="reactiveRedisTemplateForSessionIds")
    public ReactiveRedisTemplate<Long, String> reactiveRedisTemplateForSession(
            ReactiveRedisConnectionFactory factory) {
        RedisSerializationContext<Long, String> context = RedisSerializationContext
                .<Long, String> newSerializationContext(new GenericToStringSerializer<>(Long.class))
                .value(new StringRedisSerializer())
                .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, Game> reactiveRedisTemplateForGame(
            ReactiveRedisConnectionFactory factory
    ) {

        Jackson2JsonRedisSerializer<Game> serializer = new Jackson2JsonRedisSerializer<>(Game.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Game> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, Game> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, Card> reactiveRedisTemplateForCard(
            ReactiveRedisConnectionFactory factory
    ) {

        Jackson2JsonRedisSerializer<Card> valueSerializer = new Jackson2JsonRedisSerializer<>(Card.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Card> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, Card> context = builder.value(valueSerializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

}