package com.platform.cloud.mall.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Redis 原子计数器：限流、失败计数、配额共用。
 * <p>
 * 「INCR 后再 EXPIRE」分两条命令时，进程在中间崩溃会留下永不过期的 key（永久锁死），并发下也可能多次重置过期时间，
 * 所以用 Lua 脚本在 Redis 端一次完成。Redis 异常直接抛出，由调用方决定严格失败还是放行。
 */
@Component
@RequiredArgsConstructor
public class RedisCounter {

    /** 固定窗口：第一次计数时设置过期，窗口内后续计数不延长 */
    private static final RedisScript<Long> INCR_FIXED_WINDOW = new DefaultRedisScript<>(
            "local n = redis.call('INCR', KEYS[1]) "
                    + "if n == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end "
                    + "return n", Long.class);

    /** 滑动过期：每次计数都把过期时间重置（连续失败锁定：锁定期间继续尝试不会提前解锁） */
    private static final RedisScript<Long> INCR_SLIDING = new DefaultRedisScript<>(
            "local n = redis.call('INCR', KEYS[1]) "
                    + "redis.call('PEXPIRE', KEYS[1], ARGV[1]) "
                    + "return n", Long.class);

    private final StringRedisTemplate redis;

    /** 固定窗口计数，返回窗口内累计次数（含本次） */
    public long incrementInWindow(String key, Duration window) {
        return run(INCR_FIXED_WINDOW, key, window);
    }

    /** 计数并刷新过期时间，返回累计次数（含本次） */
    public long incrementSliding(String key, Duration ttl) {
        return run(INCR_SLIDING, key, ttl);
    }

    /** 撤销一次计数（如「本次请求因缺验证码被拒，不算一次密码尝试」）；不会减到负数以下的语义由调用方保证 */
    public void decrement(String key) {
        redis.opsForValue().decrement(key);
    }

    /** 当前计数；不存在为 0 */
    public long get(String key) {
        String v = redis.opsForValue().get(key);
        if (v == null) {
            return 0;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void delete(String key) {
        redis.delete(key);
    }

    private long run(RedisScript<Long> script, String key, Duration ttl) {
        Long n = redis.execute(script, List.of(key), String.valueOf(Math.max(1, ttl.toMillis())));
        return n == null ? 1 : n;
    }
}
