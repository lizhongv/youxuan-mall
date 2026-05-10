package com.platform.cloud.bootstrap.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 通过 Redis pub/sub 广播缓存失效，让多实例的 L1（Caffeine）同步失效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidator {

    public static final String TOPIC = "platform:cache:evict";
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public void broadcast(String cacheName, Object key) {
        try {
            String json = mapper.writeValueAsString(Map.of(
                    "cache", cacheName,
                    "key", key == null ? "*" : key.toString(),
                    "node", System.getProperty("sun.java.command", "unknown")
            ));
            redis.convertAndSend(TOPIC, json);
        } catch (Exception e) {
            log.warn("Failed to broadcast cache invalidation: {}", e.getMessage());
        }
    }
}
