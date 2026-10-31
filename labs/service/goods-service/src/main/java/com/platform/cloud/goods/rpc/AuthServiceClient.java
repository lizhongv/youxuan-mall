package com.platform.cloud.goods.rpc;

import com.platform.cloud.bootstrap.common.web.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 调 auth-service 的 Feign 客户端。
 * <p>{@code name} 必须与 auth-service 在 Nacos 上的注册名一致；
 * Spring Cloud LoadBalancer 会从 Nacos 拿实例列表，自动选一个发请求。
 */
@FeignClient(
        name = "auth-service",
        contextId = "authServiceClient",
        fallbackFactory = AuthServiceClientFallbackFactory.class
)
public interface AuthServiceClient {

    @GetMapping("/api/auth/users/{username}")
    R<UserInfoVO> userInfo(@PathVariable("username") String username);
}
