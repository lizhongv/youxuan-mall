package com.platform.cloud.mall.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 重置他人密码 */
@Data
public class ResetPasswordRequest {

    @NotBlank
    @Size(min = 6, max = 32, message = "密码长度需为 6~32 位")
    private String password;
}
