package com.platform.cloud.goods.rpc;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 与 auth-service 的 UserInfoVO 字段一一对应（按 JSON 字段名匹配）。
 * Phase 2 后期可抽出 platform-api-auth 共享模块避免双份维护。
 */
@Data
public class UserInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String username;
    private List<String> roles;
    private Boolean enabled;
    private Boolean accountNonLocked;
}
