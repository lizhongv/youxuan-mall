package com.platform.cloud.mall.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    /** 图形验证码 ID（GET /app/auth/captcha 返回）：每条短信都要花钱，发送前必须先过图形验证码 */
    @NotBlank(message = "请先完成图形验证码")
    @Size(max = 64)
    private String captchaId;

    @NotBlank(message = "请输入图形验证码")
    @Size(max = 16)
    private String captchaCode;
}
