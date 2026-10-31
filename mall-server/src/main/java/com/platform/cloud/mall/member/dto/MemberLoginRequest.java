package com.platform.cloud.mall.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 手机号 + 密码登录 */
@Data
public class MemberLoginRequest {

    @NotBlank
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String mobile;

    @NotBlank
    @Size(max = 64)
    private String password;

    /** 图形验证码（GET /app/auth/captcha）；连续失败 2 次后必填 */
    @Size(max = 64)
    private String captchaId;

    @Size(max = 16)
    private String captchaCode;
}
