package com.platform.cloud.mall.trade.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

/** 支付入参 */
@Data
public class OrderPayRequest {

    @NotBlank(message = "请选择支付方式")
    @Schema(description = "BALANCE 余额 / WECHAT_JSAPI 公众号内微信支付 / WECHAT_H5 浏览器微信支付 / "
            + "ALIPAY_WAP 支付宝手机网站支付 / MOCK 模拟支付；可用项见 GET /app/pay/channels")
    private String payType;

    /**
     * 余额支付密码，payType=BALANCE 时必填（服务端校验，未设置返回 60010）；其他方式忽略。
     * 不进 toString，防止被日志打印。
     */
    @ToString.Exclude
    @Schema(description = "余额支付密码（6 位数字），payType=BALANCE 时必填")
    private String payPassword;
}
