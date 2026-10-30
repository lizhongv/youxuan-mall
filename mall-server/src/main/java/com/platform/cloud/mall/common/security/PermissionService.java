package com.platform.cloud.mall.common.security;

import org.springframework.stereotype.Service;

/**
 * 管理端按钮级权限判断，供 {@code @PreAuthorize("@ss.has('goods:edit')")} 使用。
 * <p>
 * 权限码在登录时随 JWT 下发；超级管理员角色直接放行，避免每加一个按钮都要给超管补授权。
 * 权限调整后需要重新登录才生效（令牌有效期 30 分钟，可接受）。
 */
@Service("ss")
public class PermissionService {

    public boolean has(String permission) {
        if (!SecurityUtils.isAdmin()) {
            return false;
        }
        return SecurityUtils.hasAuthority(SecurityUtils.SUPER_ADMIN_ROLE) || SecurityUtils.hasAuthority(permission);
    }
}
