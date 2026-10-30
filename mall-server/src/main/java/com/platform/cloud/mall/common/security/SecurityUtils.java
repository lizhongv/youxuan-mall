package com.platform.cloud.mall.common.security;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户。JWT 的 subject 就是用户 ID（管理员 ID 或会员 ID，由角色区分）。
 * <p>
 * 业务代码查询「我的订单」「我的地址」时必须用这里取到的 ID 作为条件，不能信任前端传来的用户 ID。
 */
public final class SecurityUtils {

    public static final String SUPER_ADMIN_ROLE = "ROLE_super_admin";

    private SecurityUtils() {
    }

    /** 当前用户 ID，未登录抛 401 */
    public static Long currentUserId() {
        Long id = currentUserIdOrNull();
        if (id == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return id;
    }

    /** 当前用户 ID，未登录返回 null（公开接口里用来做「已登录则附加个人信息」） */
    public static Long currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return null;
        }
        try {
            return Long.valueOf(auth.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isAdmin() {
        return hasAuthority(UserType.ADMIN.role());
    }

    public static boolean isMember() {
        return hasAuthority(UserType.MEMBER.role());
    }

    public static boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (authority.equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
