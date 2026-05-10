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
 * 双缓存：
 * <ul>
 *     <li>@Primary {@code caffeineCacheManager}：进程内热点缓存（毫秒级）</li>
 *     <li>{@code redisCacheManager}：分布式共享缓存（Phase 2 多实例时跨节点一致）</li>
 * </ul>
 * 用法：默认 {@code @Cacheable("key")} 走 Caffeine；
 * 跨节点共享走 {@code @Cacheable(cacheManager = "redisCacheManager", value = "key")}.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
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

    private GenericJackson2JsonRedisSerializer jacksonSerializer() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(om);
    }
}
