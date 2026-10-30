package com.platform.cloud.mall.common.security;

/**
 * 令牌所属用户类型。写入 JWT 的 roles 声明为 {@code ROLE_<name>}。
 */
public enum UserType {
    ADMIN,
    MEMBER;

    public String role() {
        return "ROLE_" + name();
    }
}
