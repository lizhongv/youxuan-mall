package com.platform.cloud.bootstrap.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Token 黑名单：登出时把 jti 加入 Redis，TTL = token 剩余寿命。
 * JwtAuthenticationFilter 命中黑名单则视为未认证。
 */
@Component
@RequiredArgsConstructor
public class TokenBlacklist {

    private static final String PREFIX = "auth:bl:";

    private final StringRedisTemplate redis;

    public void revoke(String jti, Duration remaining) {
        if (remaining.isNegative() || remaining.isZero()) {
            return;
        }
        redis.opsForValue().set(PREFIX + jti, "1", remaining);
    }

    public boolean isRevoked(String jti) {
        return jti != null && Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }
}
