package com.platform.cloud.goods.web;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.goods.rpc.AuthServiceClient;
import com.platform.cloud.goods.rpc.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Phase 2.3 演示：goods-service 通过 Feign 调 auth-service。
 * 访问路径：GET /api/goods/_internal/whoami（需登录）
 */
@Slf4j
@Tag(name = "RPC Demo", description = "演示服务间调用")
@RestController
@RequestMapping("/api/goods/_internal")
@RequiredArgsConstructor
public class RpcDemoController {

    private final AuthServiceClient authClient;

    @Operation(summary = "走 Feign 反查 auth-service 的用户信息",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/whoami")
    public R<Map<String, Object>> whoami(Authentication auth) {
        String username = auth.getName();
        log.info("RPC Demo: goods-service 调 auth-service 查询 {}", username);
        R<UserInfoVO> remote = authClient.userInfo(username);
        return R.ok(Map.of(
                "callerService", "goods-service",
                "remoteService", "auth-service",
                "viaRpc", "OpenFeign + Nacos lb://",
                "remoteResponse", remote
        ));
    }
}
