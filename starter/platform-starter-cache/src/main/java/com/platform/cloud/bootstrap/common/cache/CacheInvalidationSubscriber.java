package com.platform.cloud.bootstrap.common.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 订阅 Redis 失效广播，把消息中的 cache+key 应用到本进程的 L1（清 Caffeine）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationSubscriber {

    private final RedisMessageListenerContainer container;
    private final MultiLevelCacheManager multiLevelMgr;
    private final ObjectMapper mapper;

    @PostConstruct
    public void subscribe() {
        MessageListener listener = (message, pattern) -> {
            try {
                String json = new String(message.getBody(), StandardCharsets.UTF_8);
                JsonNode n = mapper.readTree(json);
                String cache = n.path("cache").asText();
                String key = n.path("key").asText();
                if (cache.isEmpty()) return;
                multiLevelMgr.evictLocal(cache, "*".equals(key) ? null : key);
                log.debug("[CacheInvalidator] received cache={} key={}", cache, key);
            } catch (Exception e) {
                log.warn("Failed to handle cache invalidation message", e);
            }
        };
        container.addMessageListener(listener, new ChannelTopic(CacheInvalidator.TOPIC));
        log.info("Cache invalidation subscriber attached: topic={}", CacheInvalidator.TOPIC);
    }
}
