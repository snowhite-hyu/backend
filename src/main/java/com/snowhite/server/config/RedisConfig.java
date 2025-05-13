package com.snowhite.server.config;

import com.snowhite.server.domain.Room;
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

    @Bean(name="reactiveRedisTemplateForIds")
    public ReactiveRedisTemplate<String, Long> reactiveRedisTemplateForIds(
            ReactiveRedisConnectionFactory factory) {
                RedisSerializationContext<String, Long> context = RedisSerializationContext
                .<String, Long> newSerializationContext(new StringRedisSerializer())
                .value(new GenericToStringSerializer<>(Long.class))
                .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean(name="reactiveRedisTemplateForRooms")
    public ReactiveRedisTemplate<Long, Room> reactiveRedisTemplateForRooms(
            ReactiveRedisConnectionFactory factory) {
        RedisSerializationContext<Long, Room> context = RedisSerializationContext
                .<Long, Room>newSerializationContext(new GenericToStringSerializer<>(Long.class))
                .value(new Jackson2JsonRedisSerializer<>(Room.class))
                .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }
}