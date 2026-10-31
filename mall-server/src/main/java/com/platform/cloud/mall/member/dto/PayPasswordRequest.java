package com.platform.cloud.mall.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.ToString;

/**
 * 设置 / 修改余额支付密码。首次设置与修改走同一接口，都凭短信验证码：
 * 不要求原支付密码，忘记支付密码的会员也能自助重置，安全性由「本人手机号收到验证码」保证。
 */
@Data
public class PayPasswordRequest {

    @NotBlank(message = "请输入短信验证码")
    @Pattern(regexp = "^[0-9]{6}$", message = "验证码为 6 位数字")
    @Schema(description = "短信验证码，通过 POST /app/member/pay-password/sms-code 获取")
    private String code;

    /** 不进 toString，防止被日志打印 */
    @ToString.Exclude
    @NotBlank(message = "请输入支付密码")
    @Pattern(regexp = "^[0-9]{6}$", message = "支付密码为 6 位数字")
    @Schema(description = "6 位数字；不能是 6 位相同数字或 123456 / 654321 这类连续数字")
    private String payPassword;
}
