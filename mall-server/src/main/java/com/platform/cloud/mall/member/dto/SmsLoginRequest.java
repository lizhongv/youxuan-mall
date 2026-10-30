package com.platform.cloud.mall.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 手机号 + 验证码登录，未注册自动注册 */
@Data
public class SmsLoginRequest {

    @NotBlank
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String mobile;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
    private String code;
}
