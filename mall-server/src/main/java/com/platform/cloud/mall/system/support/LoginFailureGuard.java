package com.platform.cloud.mall.system.support;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 管理员登录失败计数与锁定。
 * <p>
 * 计数放 Redis 而不是表字段：多实例共享、自动过期，且不会因为暴力破解产生大量写库。
 * 每次失败都把过期时间重置为 15 分钟，因此「第 5 次失败后锁 15 分钟」，期间再试也不会提前解锁。
 * 按用户名计数（不论账号是否存在），避免通过「是否被锁」探测账号是否存在。
 */
@Component
@RequiredArgsConstructor
public class LoginFailureGuard {

    /** 连续失败上限：5 次足以容忍正常输错，又能把在线暴力破解压到每小时 20 次以内 */
    public static final int MAX_FAILURES = 5;

    /** 锁定时长 */
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final String KEY_PREFIX = "mall:login:fail:admin:";

    private final StringRedisTemplate redis;

    /** 已锁定则直接拒绝，不再校验密码 */
    public void checkNotLocked(String username) {
        if (failures(username) >= MAX_FAILURES) {
            throw new BusinessException("登录失败次数过多，账号已锁定，请 " + LOCK_DURATION.toMinutes() + " 分钟后再试");
        }
    }

    /**
     * 记录一次失败，返回给用户的提示：快锁定时提醒剩余次数，达到上限时告知已锁定。
     */
    public String onFailure(String username) {
        String key = key(username);
        Long count = redis.opsForValue().increment(key);
        redis.expire(key, LOCK_DURATION);
        long n = count == null ? 1 : count;
        if (n >= MAX_FAILURES) {
            return "用户名或密码错误次数过多，账号已锁定 " + LOCK_DURATION.toMinutes() + " 分钟";
        }
        long remaining = MAX_FAILURES - n;
        return remaining <= 2 ? "用户名或密码错误，再错 " + remaining + " 次将锁定账号" : "用户名或密码错误";
    }

    /** 登录成功或密码被重置后清零 */
    public void reset(String username) {
        redis.delete(key(username));
    }

    int failures(String username) {
        String v = redis.opsForValue().get(key(username));
        if (v == null) {
            return 0;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String key(String username) {
        // 用户名大小写不敏感地计数，防止通过变换大小写绕过锁定
        return KEY_PREFIX + username.trim().toLowerCase();
    }
}
