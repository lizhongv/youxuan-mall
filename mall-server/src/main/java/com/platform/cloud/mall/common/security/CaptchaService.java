package com.platform.cloud.mall.common.security;

import com.platform.cloud.mall.common.config.MallProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * 图形验证码：后台登录（连续失败后）与商城端发短信前使用。
 * <ul>
 *   <li>答案存 Redis 2 分钟，按场景（admin / app）隔离，商城端拿到的验证码不能用于后台登录。</li>
 *   <li>一次性：校验时用 GETDEL 原子取出并删除，无论对错都作废，防止对同一张图反复猜。</li>
 * </ul>
 * Redis 异常直接抛出（严格失败）：验证码保护的是登录与短信费用，宁可暂时不可用也不能被绕过。
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    public static final String SCOPE_ADMIN = "admin";
    public static final String SCOPE_APP = "app";

    static final Duration TTL = Duration.ofMinutes(2);
    /** 4 位：人工输入负担小；配合一次性与限流，随机猜中概率为万分之一 */
    static final int LENGTH = 4;
    /** 去掉形近的 0/1/7，七段笔画下它们容易被看错 */
    private static final String ALPHABET = "2345689";

    private static final String PREFIX = "mall:captcha:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final MallProperties props;

    public CaptchaVO create(String scope) {
        String id = UUID.randomUUID().toString().replace("-", "");
        String code = randomCode();
        redis.opsForValue().set(key(scope, id), code, TTL);
        String image = "data:image/png;base64," + Base64.getEncoder().encodeToString(CaptchaImageRenderer.renderPng(code));
        return new CaptchaVO(id, image, props.getSecurity().isCaptchaEcho() ? code : null);
    }

    /** 校验并作废；id 或答案为空直接返回 false */
    public boolean verify(String scope, String captchaId, String answer) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(answer) || captchaId.length() > 64) {
            return false;
        }
        String stored = redis.opsForValue().getAndDelete(key(scope, captchaId.trim()));
        if (stored == null) {
            return false;
        }
        return MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8),
                answer.trim().getBytes(StandardCharsets.UTF_8));
    }

    static String randomCode() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String key(String scope, String id) {
        return PREFIX + scope + ":" + id;
    }
}
