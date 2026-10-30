package com.platform.cloud.mall.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 发送短信验证码 */
@Data
public class SmsCodeRequest {

    @NotBlank
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String mobile;

    @NotBlank
    @Pattern(regexp = "^(REGISTER|LOGIN|RESET)$", message = "场景只能是 REGISTER / LOGIN / RESET")
    private String scene;
}
