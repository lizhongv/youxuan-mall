package com.platform.cloud.bootstrap.common.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多级 CacheManager：每次 getCache(name) 时，从底层的 Caffeine 和 Redis 各拿一个同名 Cache，
 * 包成 MultiLevelCache 返回。
 */
public class MultiLevelCacheManager implements CacheManager {

    private final CacheManager caffeineMgr;
    private final CacheManager redisMgr;
    private final CacheInvalidator invalidator;
    private final ConcurrentHashMap<String, MultiLevelCache> caches = new ConcurrentHashMap<>();

    public MultiLevelCacheManager(CacheManager caffeineMgr, CacheManager redisMgr, CacheInvalidator invalidator) {
        this.caffeineMgr = caffeineMgr;
        this.redisMgr = redisMgr;
        this.invalidator = invalidator;
    }

    @Override
    @Nullable
    public Cache getCache(@NonNull String name) {
        return caches.computeIfAbsent(name, n -> {
            Cache l1 = caffeineMgr.getCache(n);
            Cache l2 = redisMgr.getCache(n);
            return new MultiLevelCache(n, l1, l2, invalidator);
        });
    }

    @Override
    @NonNull
    public Collection<String> getCacheNames() {
        Set<String> names = new java.util.HashSet<>(caffeineMgr.getCacheNames());
        names.addAll(redisMgr.getCacheNames());
        names.addAll(caches.keySet());
        return names;
    }

    /** 收到广播，清掉对应 L1（不动 L2 —— L2 是共享的，已经被发出方清了） */
    public void evictLocal(String cacheName, Object key) {
        MultiLevelCache c = caches.get(cacheName);
        if (c != null) {
            c.evictLocal(key);
        }
    }
}
