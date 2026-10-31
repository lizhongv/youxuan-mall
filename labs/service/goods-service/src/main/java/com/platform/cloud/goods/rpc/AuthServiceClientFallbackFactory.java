package com.platform.cloud.goods.rpc;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Feign 调用 auth-service 失败时的兜底（auth-service 离线、超时、5xx 等）。
 * Phase 2.4 接入 Sentinel 后，会从单纯的兜底升级为熔断 + 限流 + 降级。
 */
@Slf4j
@Component
public class AuthServiceClientFallbackFactory implements FallbackFactory<AuthServiceClient> {

    @Override
    public AuthServiceClient create(Throwable cause) {
        log.warn("AuthServiceClient fallback triggered: {}", cause.getMessage());
        return username -> R.fail(ResultCode.SERVICE_UNAVAILABLE,
                "auth-service 不可用 (fallback): " + cause.getClass().getSimpleName());
    }
}
