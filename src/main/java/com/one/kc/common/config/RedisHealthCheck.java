package com.one.kc.common.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthCheck {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisHealthCheck(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkRedis() {
        redisTemplate.opsForValue().set("health", "ok");
    }
}


