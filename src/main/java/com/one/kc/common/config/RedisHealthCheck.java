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

    private static final Logger log = LoggerFactory.getLogger(RedisHealthCheck.class);
    private final RedisTemplate<String, String> redisTemplate;

    public RedisHealthCheck(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkRedis() {
        int attempts = 0;
        while (attempts < 10) {
            try {
                redisTemplate.opsForValue().set("health", "ok");
                log.info("Redis is reachable");
                return;
            } catch (Exception e) {
                attempts++;
                log.warn("Redis not ready yet, retrying... ({}/10)", attempts);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
            }
        }
        log.error("Redis not reachable after retries — continuing without failing startup");
    }
}


