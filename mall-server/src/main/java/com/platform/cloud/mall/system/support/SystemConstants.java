package com.platform.cloud.mall.system.support;

/**
 * 系统领域内的约定常量。
 */
public final class SystemConstants {

    /** 内置超级管理员账号，首次启动自动创建，不允许停用或删除 */
    public static final String ADMIN_USERNAME = "admin";

    /** 超级管理员角色编码，对应令牌里的 {@code ROLE_super_admin} */
    public static final String SUPER_ADMIN_CODE = "super_admin";

    /** 令牌中标识用户类型的声明，refresh 时据此拒绝商城端令牌 */
    public static final String CLAIM_UTYPE = "utype";

    /** 角色编码在令牌里的前缀，与 Spring Security hasRole 的约定一致 */
    public static final String ROLE_PREFIX = "ROLE_";

    public static final int ENABLED = 1;
    public static final int DISABLED = 0;

    private SystemConstants() {
    }
}
