package com.platform.cloud.mall.common.security;

import com.platform.cloud.bootstrap.common.mybatis.CurrentUserIdProvider;
import org.springframework.stereotype.Component;

/**
 * 把当前登录用户 ID 提供给审计字段自动填充。
 */
@Component
public class SecurityAuditorProvider implements CurrentUserIdProvider {

    @Override
    public Long currentUserId() {
        return SecurityUtils.currentUserIdOrNull();
    }
}
