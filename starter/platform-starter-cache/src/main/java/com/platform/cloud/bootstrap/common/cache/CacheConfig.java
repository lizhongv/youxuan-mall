package com.platform.cloud.bootstrap.common.cache;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 三档 CacheManager：
 * <ul>
 *     <li>@Primary {@code multiLevelCacheManager}：Caffeine L1 + Redis L2，所有 @Cacheable 默认用</li>
 *     <li>{@code caffeineCacheManager}：纯进程内（极致快但不跨节点）</li>
 *     <li>{@code redisCacheManager}：纯分布式（跨节点一致但有网络往返）</li>
 * </ul>
 * 想强制单层用 {@code @Cacheable(cacheManager = "caffeineCacheManager", ...)} 即可。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager();
        mgr.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(64)
                .maximumSize(10_000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats());
        return mgr;
    }

    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory cf) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jacksonSerializer()))
                .disableCachingNullValues()
                .prefixCacheNameWith("platform:cache:");
        return RedisCacheManager.builder(cf).cacheDefaults(config).build();
    }

    @Bean
    @Primary
    public MultiLevelCacheManager multiLevelCacheManager(
            CacheManager caffeineCacheManager,
            CacheManager redisCacheManager,
            CacheInvalidator invalidator) {
        return new MultiLevelCacheManager(caffeineCacheManager, redisCacheManager, invalidator);
    }

    /** Redis pub/sub 监听容器，给 CacheInvalidationSubscriber 使用 */
    @Bean
    public org.springframework.data.redis.listener.RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory cf) {
        var c = new org.springframework.data.redis.listener.RedisMessageListenerContainer();
        c.setConnectionFactory(cf);
        return c;
    }

    private GenericJackson2JsonRedisSerializer jacksonSerializer() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(om);
    }
}
