package com.platform.cloud.mall.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送设置支付密码的短信验证码。手机号取当前登录会员的，不由前端传入，防止借此给他人手机号发短信。
 * 仍要求图形验证码：每条短信都要花钱，登录态被盗用或脚本调用时也不能刷短信。
 */
@Data
public class PayPasswordSmsRequest {

    @NotBlank(message = "请先完成图形验证码")
    @Size(max = 64)
    private String captchaId;

    @NotBlank(message = "请输入图形验证码")
    @Size(max = 16)
    private String captchaCode;
}
