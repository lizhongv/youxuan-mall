package com.platform.cloud.bootstrap.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Token 黑名单：登出时把 jti 加入 Redis，TTL = token 剩余寿命。
 * JwtAuthenticationFilter 命中黑名单则视为未认证。
 * <p>
 * Redis 异常不在这里吞掉：access 令牌检查由过滤器决定放行（可用性优先），refresh 令牌由调用方严格失败。
 */
@Component
@RequiredArgsConstructor
public class TokenBlacklist {

    private static final String PREFIX = "auth:bl:";

    private final StringRedisTemplate redis;

    public void revoke(String jti, Duration remaining) {
        if (jti == null || remaining.isNegative() || remaining.isZero()) {
            return;
        }
        redis.opsForValue().set(PREFIX + jti, "1", remaining);
    }

    public boolean isRevoked(String jti) {
        return jti != null && Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }

    /**
     * 一次性消费（refresh 令牌轮换用）：原子地 SET NX 把 jti 拉黑，只有第一个调用者返回 true。
     * <p>
     * 不能写成「先 isRevoked 再 revoke」：两个并发请求会同时通过检查，各自换到一对新令牌，
     * 被窃取的 refresh 令牌就能与合法用户并行使用。已登出（已拉黑）的令牌同样返回 false。
     */
    public boolean tryConsume(String jti, Duration remaining) {
        if (jti == null || remaining.isNegative() || remaining.isZero()) {
            return false;
        }
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(PREFIX + jti, "1", remaining));
    }
}
