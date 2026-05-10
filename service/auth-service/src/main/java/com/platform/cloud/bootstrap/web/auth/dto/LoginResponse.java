package com.platform.cloud.bootstrap.web.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "Access Token（用于业务接口）")
    private String accessToken;

    @Schema(description = "Refresh Token（用于刷新 access token）")
    private String refreshToken;

    @Schema(description = "Token 类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "access token 有效秒数")
    private long expiresIn;
}
