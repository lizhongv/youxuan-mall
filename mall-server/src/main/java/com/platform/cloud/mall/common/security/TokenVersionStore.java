package com.platform.cloud.mall.common.security;

import com.platform.cloud.bootstrap.common.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 令牌版本号：签发时写入 {@code tv} 声明，每次请求比对当前版本，不一致即令牌已作废。
 * <p>
 * 当前版本缓存在 Redis（每个请求都要查，不能每次打数据库），未命中回源数据库。
 * 变更流程：数据库自增 → 删除缓存；事务内调用时提交后再删一次，避免「事务未提交时别的请求把旧值重新读进缓存」。
 * 缓存 TTL 只是兜底（删除缓存失败时最多这么久后自愈）。
 */
@Slf4j
@Component
public class TokenVersionStore {

    static final Duration CACHE_TTL = Duration.ofMinutes(10);
    /** 用户不存在时缓存的值：任何令牌都对不上 */
    static final String MISSING = "-1";
    private static final String PREFIX = "mall:tv:";

    private final StringRedisTemplate redis;
    private final Map<UserType, TokenVersionSource> sources = new EnumMap<>(UserType.class);

    public TokenVersionStore(StringRedisTemplate redis, List<TokenVersionSource> sources) {
        this.redis = redis;
        sources.forEach(s -> this.sources.put(s.userType(), s));
    }

    /** 当前版本号；用户不存在返回 -1。Redis 异常直接抛出，由调用方决定策略 */
    public int current(UserType type, Long userId) {
        String key = key(type, userId);
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            return parse(cached);
        }
        TokenVersionSource source = sources.get(type);
        if (source == null) {
            // 该类用户未接入版本号：视为恒为 0，与未带 tv 的令牌兼容
            return 0;
        }
        Integer v = source.loadVersion(userId);
        String value = v == null ? MISSING : String.valueOf(v);
        redis.opsForValue().set(key, value, CACHE_TTL);
        return parse(value);
    }

    /** 令牌里的版本号是否为当前版本 */
    public boolean matches(UserType type, Long userId, Claims claims) {
        return current(type, userId) == versionOf(claims);
    }

    /**
     * 作废该用户全部已签发令牌：数据库自增 + 删缓存。必须在修改密码 / 状态 / 角色的同一事务内调用。
     */
    public void revokeAll(UserType type, Long userId) {
        TokenVersionSource source = sources.get(type);
        if (source == null) {
            throw new IllegalStateException("未注册令牌版本号来源: " + type);
        }
        source.increment(userId);
        evict(type, userId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict(type, userId);
                }
            });
        }
    }

    /** 令牌中的 tv 声明；升级前签发的令牌没有该声明，按 0 处理（与数据库默认值一致） */
    public static int versionOf(Claims claims) {
        Object v = claims.get(JwtService.CLAIM_TOKEN_VERSION);
        return v instanceof Number n ? n.intValue() : 0;
    }

    private void evict(UserType type, Long userId) {
        try {
            redis.delete(key(type, userId));
        } catch (RuntimeException e) {
            // 删不掉缓存时旧令牌最多在 CACHE_TTL 内仍可用；不能因此让改密 / 冻结操作失败
            log.error("删除令牌版本号缓存失败 type={} id={}: {}", type, userId, e.getMessage());
        }
    }

    private static int parse(String v) {
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String key(UserType type, Long userId) {
        return PREFIX + type.name().toLowerCase() + ":" + userId;
    }
}
