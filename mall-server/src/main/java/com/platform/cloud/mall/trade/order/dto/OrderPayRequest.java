package com.platform.cloud.mall.trade.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 支付入参 */
@Data
public class OrderPayRequest {

    @NotBlank(message = "请选择支付方式")
    @Schema(description = "BALANCE 余额 / MOCK 模拟支付；WECHAT / ALIPAY 预留")
    private String payType;
}
