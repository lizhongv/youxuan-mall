package com.platform.cloud.mall.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改自己的密码 */
@Data
public class ChangePasswordRequest {

    @NotBlank
    private String oldPassword;

    /** 6~32 位：BCrypt 只取前 72 字节，再长没有意义 */
    @NotBlank
    @Size(min = 6, max = 32, message = "密码长度需为 6~32 位")
    private String newPassword;
}
