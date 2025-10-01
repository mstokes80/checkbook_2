package com.checkbook.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Configuration for rate limiting functionality
 * Uses in-memory storage for development, Redis for production
 */
@Configuration
public class RateLimitingConfig {

    /**
     * In-memory rate limiting storage for development
     * In production, this should be replaced with Redis
     */
    @Bean
    public ConcurrentMap<String, Object> rateLimitStorage() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Redis template configuration for distributed rate limiting
     * Uncomment and configure for production use with Redis
     */
    /*
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory("localhost", 6379);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
    */
}