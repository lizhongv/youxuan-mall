package com.platform.cloud.mall.common.security;

import com.platform.cloud.bootstrap.common.security.TokenVersionChecker;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 接入 starter 过滤器的版本号检查：按令牌的 utype 声明区分管理员 / 会员。
 * Redis 异常向上抛给过滤器，由过滤器按「access 令牌可用性优先」放行。
 */
@Component
@RequiredArgsConstructor
public class MallTokenVersionChecker implements TokenVersionChecker {

    /** 与 SystemConstants / MemberConstants 中的 CLAIM_UTYPE 一致 */
    public static final String CLAIM_UTYPE = "utype";

    private final TokenVersionStore store;

    @Override
    public boolean isCurrent(Claims claims) {
        UserType type = userType(claims);
        Long userId = userId(claims);
        if (type == null || userId == null) {
            return false;
        }
        return store.matches(type, userId, claims);
    }

    static UserType userType(Claims claims) {
        Object v = claims.get(CLAIM_UTYPE);
        if (v == null) {
            return null;
        }
        try {
            return UserType.valueOf(v.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static Long userId(Claims claims) {
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }
}
