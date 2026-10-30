package com.platform.cloud.mall.member.support;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 会员密码登录失败锁定，规则与管理端一致：连续失败 5 次锁 15 分钟。
 * 锁定只影响密码登录，用户仍可用验证码登录或重置密码自助解锁。
 */
@Component
@RequiredArgsConstructor
public class MemberLoginGuard {

    public static final int MAX_FAILURES = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final String KEY_PREFIX = "mall:login:fail:member:";

    private final StringRedisTemplate redis;

    public void checkNotLocked(String mobile) {
        String v = redis.opsForValue().get(KEY_PREFIX + mobile);
        if (v != null && parse(v) >= MAX_FAILURES) {
            throw new BusinessException("密码错误次数过多，请 " + LOCK_DURATION.toMinutes() + " 分钟后再试，或使用验证码登录");
        }
    }

    /** 记录一次失败，返回给用户的提示 */
    public String onFailure(String mobile) {
        String key = KEY_PREFIX + mobile;
        Long count = redis.opsForValue().increment(key);
        redis.expire(key, LOCK_DURATION);
        long n = count == null ? 1 : count;
        if (n >= MAX_FAILURES) {
            return "密码错误次数过多，已锁定 " + LOCK_DURATION.toMinutes() + " 分钟，可使用验证码登录";
        }
        long remaining = MAX_FAILURES - n;
        return remaining <= 2 ? "手机号或密码错误，再错 " + remaining + " 次将被锁定" : "手机号或密码错误";
    }

    public void reset(String mobile) {
        redis.delete(KEY_PREFIX + mobile);
    }

    private static int parse(String v) {
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
