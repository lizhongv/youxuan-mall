package com.platform.cloud.bootstrap.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.lang.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.LongAdder;

/**
 * 二级缓存：先查 L1（Caffeine 进程内）→ miss 再查 L2（Redis 跨节点）→ 都 miss 走 loader（通常进 DB）。
 * 写/失效时同时清两层 + 通过 Redis pub/sub 广播让其他节点的 L1 也失效。
 */
@Slf4j
public class MultiLevelCache implements Cache {

    private final String name;
    private final Cache l1;       // Caffeine
    private final Cache l2;       // Redis
    private final CacheInvalidator invalidator;

    private final LongAdder l1Hits = new LongAdder();
    private final LongAdder l2Hits = new LongAdder();
    private final LongAdder misses = new LongAdder();

    public MultiLevelCache(String name, Cache l1, Cache l2, CacheInvalidator invalidator) {
        this.name = name;
        this.l1 = l1;
        this.l2 = l2;
        this.invalidator = invalidator;
    }

    @Override public @NonNull String getName() { return name; }
    @Override public @NonNull Object getNativeCache() { return this; }

    @Override
    public ValueWrapper get(@NonNull Object key) {
        ValueWrapper v = l1.get(key);
        if (v != null) {
            l1Hits.increment();
            log.debug("[{}] L1 hit  key={}", name, key);
            return v;
        }
        v = l2.get(key);
        if (v != null) {
            l2Hits.increment();
            log.debug("[{}] L2 hit  key={} → 回填 L1", name, key);
            l1.put(key, v.get());          // 把 L2 命中的值回填到 L1
            return v;
        }
        misses.increment();
        log.debug("[{}] miss     key={}", name, key);
        return null;
    }

    @Override
    public <T> T get(@NonNull Object key, Class<T> type) {
        ValueWrapper v = get(key);
        return v == null ? null : type.cast(v.get());
    }

    @Override
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        ValueWrapper v = get(key);
        if (v != null) {
            @SuppressWarnings("unchecked") T cached = (T) v.get();
            return cached;
        }
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(@NonNull Object key, Object value) {
        l1.put(key, value);
        l2.put(key, value);
        log.debug("[{}] put      key={}", name, key);
    }

    @Override
    public void evict(@NonNull Object key) {
        l1.evict(key);
        l2.evict(key);
        invalidator.broadcast(name, key);
        log.debug("[{}] evict    key={} (broadcast to other nodes)", name, key);
    }

    @Override
    public void clear() {
        l1.clear();
        l2.clear();
        invalidator.broadcast(name, null);
    }

    /** 接收到其他节点广播时，仅清自己 L1（不动 L2，因为 L2 是全节点共享的） */
    public void evictLocal(Object key) {
        if (key == null) l1.clear();
        else l1.evict(key);
    }

    public Stats stats() {
        return new Stats(name, l1Hits.sum(), l2Hits.sum(), misses.sum());
    }

    public record Stats(String cache, long l1Hits, long l2Hits, long misses) {}
}
