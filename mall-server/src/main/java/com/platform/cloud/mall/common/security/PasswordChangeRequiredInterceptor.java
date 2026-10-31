package com.platform.cloud.mall.common.security;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.security.JwtAuthenticationFilter;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.mall.common.config.MallProperties;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 强制修改初始密码：管理员令牌带 {@code mcp=true} 声明时，除白名单外的管理端接口返回 40301。
 * <p>
 * 标记放在令牌里而不是每次查库：改密码会自增令牌版本号，旧令牌（带 mcp）随之作废，新令牌不再带标记，状态天然一致。
 */
@Component
@RequiredArgsConstructor
public class PasswordChangeRequiredInterceptor implements HandlerInterceptor {

    /** 令牌声明：必须修改密码 */
    public static final String CLAIM_MUST_CHANGE_PASSWORD = "mcp";

    /** 拦截范围 */
    public static final String PATH_PATTERN = "/admin/**";

    /** 改密码流程本身需要的接口：看自己是谁、改密码、登出 */
    public static final List<String> ALLOWED_PATHS = List.of(
            "/admin/auth/me", "/admin/auth/password", "/admin/auth/logout",
            "/admin/auth/login", "/admin/auth/refresh", "/admin/auth/captcha");

    private final MallProperties props;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!props.getSecurity().isForceChangeInitialPassword()) {
            return true;
        }
        Object attr = request.getAttribute(JwtAuthenticationFilter.CLAIMS_ATTRIBUTE);
        if (attr instanceof Claims claims && mustChange(claims)) {
            throw new BusinessException(ResultCode.PASSWORD_CHANGE_REQUIRED);
        }
        return true;
    }

    static boolean mustChange(Claims claims) {
        return UserType.ADMIN.name().equals(claims.get(MallTokenVersionChecker.CLAIM_UTYPE))
                && Boolean.TRUE.equals(claims.get(CLAIM_MUST_CHANGE_PASSWORD));
    }
}
