package com.platform.cloud.bootstrap.common.mybatis;

/**
 * 审计字段的当前用户来源。由使用方（如 mall-server）按自己的登录体系实现并注册为 Bean；
 * 未注册时审计人记为匿名，这样 starter-mybatis 不必依赖 spring-security。
 */
@FunctionalInterface
public interface CurrentUserIdProvider {

    /** 当前操作人 ID；无登录上下文（定时任务、未登录请求）时返回 null */
    Long currentUserId();
}
