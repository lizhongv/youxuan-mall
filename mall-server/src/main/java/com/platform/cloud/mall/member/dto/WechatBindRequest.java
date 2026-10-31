package com.platform.cloud.mall.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 微信首次登录绑定手机号：验证码场景用 LOGIN（未注册的手机号自动注册） */
@Data
public class WechatBindRequest {

    @NotBlank
    @Size(max = 64)
    private String bindToken;

    @NotBlank
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String mobile;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
    private String code;
}
