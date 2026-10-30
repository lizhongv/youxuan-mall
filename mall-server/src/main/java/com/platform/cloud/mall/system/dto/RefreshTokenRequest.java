package com.platform.cloud.mall.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 刷新令牌 */
@Data
public class RefreshTokenRequest {

    @NotBlank
    private String refreshToken;
}
