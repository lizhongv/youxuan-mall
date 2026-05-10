package com.platform.cloud.bootstrap.web.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "刷新令牌请求")
public class RefreshRequest {

    @NotBlank(message = "不能为空")
    @Schema(description = "Refresh Token")
    private String refreshToken;
}
