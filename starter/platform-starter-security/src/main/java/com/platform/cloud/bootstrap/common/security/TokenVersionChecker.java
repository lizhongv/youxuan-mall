package com.platform.cloud.bootstrap.common.security;

import io.jsonwebtoken.Claims;

/**
 * 令牌版本号校验回调（可选 Bean）。
 * <p>
 * JWT 无状态，签发后无法单独作废；改密、冻结、停用、调整角色后需要让该用户「所有已签发令牌」立即失效。
 * 做法是签发时写入 {@code tv}（{@link JwtService#CLAIM_TOKEN_VERSION}）声明，业务方在上述事件发生时自增用户的版本号，
 * 过滤器每次请求回调本接口比对。starter 不关心版本号存在哪、属于哪类用户，因此只定义接口，由业务应用实现，
 * 保持 starter 不依赖任何业务表。
 * <p>
 * 未提供实现时过滤器跳过此项检查（兼容未接入版本号的服务）。
 */
public interface TokenVersionChecker {

    /**
     * 令牌中的版本号是否仍是该用户的当前版本。
     *
     * @param claims 已通过签名与过期校验的 access 令牌声明
     * @return false 表示令牌已被作废，过滤器按未登录处理
     * @throws RuntimeException 版本号存储（如 Redis）不可用时可直接抛出，由过滤器按可用性优先策略处理
     */
    boolean isCurrent(Claims claims);
}
