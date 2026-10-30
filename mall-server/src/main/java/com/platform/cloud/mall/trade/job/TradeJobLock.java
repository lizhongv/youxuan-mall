package com.platform.cloud.mall.trade.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 定时任务的分布式互斥：多实例部署时同一分钟只让一个实例执行。
 * <p>
 * 用 SETNX + 过期时间实现，不主动释放：锁 50 秒后自然过期，下一分钟的调度再竞争。
 * 这样即使某次执行跨过分钟边界也不会被另一实例并发执行；而业务本身是条件更新，偶发重入也不会出错。
 * Redis 不可用时放弃本轮（宁可晚一分钟关单，也不让多实例同时跑）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeJobLock {

    static final String PREFIX = "mall:job:lock:";
    /** 小于调度周期 60 秒，保证下一轮能重新竞争 */
    static final Duration TTL = Duration.ofSeconds(50);

    private final StringRedisTemplate redis;
    private final String instanceId = UUID.randomUUID().toString();

    public boolean tryLock(String job) {
        try {
            Boolean ok = redis.opsForValue().setIfAbsent(PREFIX + job, instanceId, TTL);
            return Boolean.TRUE.equals(ok);
        } catch (RuntimeException e) {
            log.warn("获取任务锁失败，本轮跳过 job={}", job, e);
            return false;
        }
    }
}
