package com.platform.cloud.mall.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 验证码重置密码 */
@Data
public class MemberResetPasswordRequest {

    @NotBlank
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String mobile;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
    private String code;

    @NotBlank
    @Size(min = 6, max = 32, message = "密码长度需为 6~32 位")
    private String password;
}
