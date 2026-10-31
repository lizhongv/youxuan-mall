package com.platform.cloud.mall.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员登录 */
@Data
public class AdminLoginRequest {

    @NotBlank
    @Size(max = 64)
    private String username;

    @NotBlank
    @Size(max = 64)
    private String password;

    /** 图形验证码 ID（GET /admin/auth/captcha 返回）；登录失败 2 次后必填 */
    @Size(max = 64)
    private String captchaId;

    /** 图形验证码答案 */
    @Size(max = 16)
    private String captchaCode;
}
