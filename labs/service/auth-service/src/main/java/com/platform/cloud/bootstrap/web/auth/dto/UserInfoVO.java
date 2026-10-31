package com.platform.cloud.bootstrap.web.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 内部 RPC 用：把当前用户信息暴露给其他 service。
 * Phase 2.3 起在 goods-service 里有同字段的 mirror DTO（按 JSON 字段名匹配）；
 * Phase 2 后期可抽出 platform-api-auth 共享模块。
 */
@Data
@Builder
@Schema(description = "用户信息（内部 RPC）")
public class UserInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String username;
    private List<String> roles;
    private Boolean enabled;
    private Boolean accountNonLocked;
}
