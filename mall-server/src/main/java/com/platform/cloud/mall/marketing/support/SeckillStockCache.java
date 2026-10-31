package com.platform.cloud.mall.marketing.support;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀名额的 Redis 侧：活动剩余库存（String）+ 每人已抢数量（Hash）。
 * <p>
 * 抢购时用 Lua 在一次原子操作里完成「库存够 且 个人未超限」的判断和扣减，挡住绝大部分并发请求；
 * 数据库 mkt_seckill.sold 的条件更新是最终防线，即使 Redis 数据异常也不会超卖。
 */
@Component
@RequiredArgsConstructor
public class SeckillStockCache {

    /** Lua 返回码：库存键不存在（未预热或已过期） */
    public static final long NOT_WARMED = -1L;
    /** Lua 返回码：已售罄 */
    public static final long SOLD_OUT = -2L;
    /** Lua 返回码：剩余数量不足本次购买数量 */
    public static final long INSUFFICIENT = -3L;
    /** Lua 返回码：超出个人限购 */
    public static final long OVER_LIMIT = -4L;

    static final String STOCK_KEY = "mall:seckill:stock:";
    static final String BOUGHT_KEY = "mall:seckill:bought:";

    /** 活动结束后键再保留 1 天，便于排查；之后由 Redis 自动清理 */
    static final Duration KEEP_AFTER_END = Duration.ofDays(1);

    /**
     * KEYS[1] 库存键，KEYS[2] 个人已购 Hash；ARGV[1] 会员 ID，ARGV[2] 数量，ARGV[3] 限购。
     * 返回扣减后的剩余库存，或上面的负数错误码。
     */
    private static final RedisScript<Long> ACQUIRE = new DefaultRedisScript<>("""
            local stock = redis.call('GET', KEYS[1])
            if not stock then return -1 end
            stock = tonumber(stock)
            local q = tonumber(ARGV[2])
            if stock <= 0 then return -2 end
            if stock < q then return -3 end
            local bought = tonumber(redis.call('HGET', KEYS[2], ARGV[1]) or '0')
            if bought + q > tonumber(ARGV[3]) then return -4 end
            redis.call('HINCRBY', KEYS[2], ARGV[1], q)
            return redis.call('DECRBY', KEYS[1], q)
            """, Long.class);

    /**
     * 归还名额：库存键存在才加回（活动已结束过期的不用管）；个人已购减到 0 为止，防止重复归还变负数。
     */
    private static final RedisScript<Long> RELEASE = new DefaultRedisScript<>("""
            local q = tonumber(ARGV[2])
            if redis.call('EXISTS', KEYS[1]) == 1 then redis.call('INCRBY', KEYS[1], q) end
            local bought = tonumber(redis.call('HGET', KEYS[2], ARGV[1]) or '0')
            if bought > 0 then
              local left = bought - q
              if left > 0 then redis.call('HSET', KEYS[2], ARGV[1], left)
              else redis.call('HDEL', KEYS[2], ARGV[1]) end
            end
            return 1
            """, Long.class);

    /**
     * 按差量调整剩余库存：键存在才 INCRBY，并刷新两个键的过期时间；返回 1 表示已调整，0 表示键不存在。
     * 用差量而不是覆盖写，是为了不吞掉调整期间其他请求在 Redis 上的扣减 / 归还。
     */
    private static final RedisScript<Long> ADJUST = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then return 0 end
            redis.call('INCRBY', KEYS[1], tonumber(ARGV[1]))
            redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[2]))
            if redis.call('EXISTS', KEYS[2]) == 1 then redis.call('PEXPIRE', KEYS[2], tonumber(ARGV[2])) end
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;

    /** 原子抢占名额，返回剩余库存或错误码 */
    public long tryAcquire(Long seckillId, Long memberId, int quantity, int limitPerMember) {
        Long r = redis.execute(ACQUIRE, List.of(STOCK_KEY + seckillId, BOUGHT_KEY + seckillId),
                String.valueOf(memberId), String.valueOf(quantity), String.valueOf(limitPerMember));
        return r == null ? NOT_WARMED : r;
    }

    public void release(Long seckillId, Long memberId, int quantity) {
        redis.execute(RELEASE, List.of(STOCK_KEY + seckillId, BOUGHT_KEY + seckillId),
                String.valueOf(memberId), String.valueOf(quantity));
    }

    /**
     * 预热（覆盖写）剩余库存 = 活动库存 - 已售。以数据库为准，管理端修改库存或应用启动时调用。
     */
    public void warm(Long seckillId, int remaining, LocalDateTime endAt) {
        Duration ttl = ttl(endAt);
        redis.opsForValue().set(STOCK_KEY + seckillId, String.valueOf(Math.max(remaining, 0)), ttl);
        redis.expire(BOUGHT_KEY + seckillId, ttl);
    }

    /**
     * 管理端修改活动库存后按差量同步（delta = 新活动库存 − 旧活动库存）。
     *
     * @return false 表示键不存在（未预热 / 已过期），调用方应以数据库最新值 {@link #warmIfAbsent} 补齐
     */
    public boolean adjust(Long seckillId, int delta, LocalDateTime endAt) {
        Long r = redis.execute(ADJUST, List.of(STOCK_KEY + seckillId, BOUGHT_KEY + seckillId),
                String.valueOf(delta), String.valueOf(ttl(endAt).toMillis()));
        return r != null && r == 1L;
    }

    /** 懒加载预热：键不存在时才写，避免覆盖并发中其他请求的扣减 */
    public void warmIfAbsent(Long seckillId, int remaining, LocalDateTime endAt) {
        redis.opsForValue().setIfAbsent(STOCK_KEY + seckillId, String.valueOf(Math.max(remaining, 0)), ttl(endAt));
    }

    public void remove(Long seckillId) {
        redis.delete(List.of(STOCK_KEY + seckillId, BOUGHT_KEY + seckillId));
    }

    private static Duration ttl(LocalDateTime endAt) {
        Duration d = Duration.between(LocalDateTime.now(), endAt).plus(KEEP_AFTER_END);
        return d.isNegative() || d.isZero() ? KEEP_AFTER_END : d;
    }
}
