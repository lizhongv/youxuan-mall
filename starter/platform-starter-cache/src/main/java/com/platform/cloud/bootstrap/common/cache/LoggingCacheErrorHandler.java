package com.platform.cloud.bootstrap.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.lang.NonNull;

/**
 * 缓存异常降级：只记日志不抛出。
 * <ul>
 *   <li>get 失败：Spring 视为未命中，执行被注解的方法回源（通常查库）</li>
 *   <li>put 失败：结果照常返回，只是本次没有缓存</li>
 *   <li>evict / clear 失败：可能短时间读到旧值（受缓存 TTL 约束），记 error 便于排查</li>
 * </ul>
 * Spring 默认的 SimpleCacheErrorHandler 会直接抛出，Redis 一抖首页等接口就 500。
 */
@Slf4j
public class LoggingCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(@NonNull RuntimeException e, @NonNull Cache cache, @NonNull Object key) {
        log.warn("缓存读取失败，回源处理 cache={} key={}: {}", cache.getName(), key, e.getMessage());
    }

    @Override
    public void handleCachePutError(@NonNull RuntimeException e, @NonNull Cache cache, @NonNull Object key,
                                    Object value) {
        log.warn("缓存写入失败 cache={} key={}: {}", cache.getName(), key, e.getMessage());
    }

    @Override
    public void handleCacheEvictError(@NonNull RuntimeException e, @NonNull Cache cache, @NonNull Object key) {
        log.error("缓存失效失败，可能短时读到旧数据 cache={} key={}: {}", cache.getName(), key, e.getMessage());
    }

    @Override
    public void handleCacheClearError(@NonNull RuntimeException e, @NonNull Cache cache) {
        log.error("缓存清空失败，可能短时读到旧数据 cache={}: {}", cache.getName(), e.getMessage());
    }
}
