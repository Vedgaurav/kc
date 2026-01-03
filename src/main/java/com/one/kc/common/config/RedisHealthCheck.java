package com.one.kc.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthCheck {

    Logger logger = LoggerFactory.getLogger(RedisHealthCheck.class);

    public RedisHealthCheck(RedisTemplate<String, String> redisTemplate) {
        redisTemplate.opsForValue().set("ping", "pong");
        logger.info("Redis connected: {}", redisTemplate.opsForValue().get("ping"));
    }
}

