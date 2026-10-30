package com.platform.cloud.mall.trade.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 后台改价：adjustAmount 为相对原应付的总调整额（覆盖上一次改价），负数为减价 */
@Data
public class OrderPriceRequest {

    @NotNull(message = "调整金额不能为空")
    @Digits(integer = 8, fraction = 2, message = "金额最多两位小数")
    @DecimalMin(value = "-99999999.99", message = "调整金额超出范围")
    @DecimalMax(value = "99999999.99", message = "调整金额超出范围")
    @Schema(description = "调整金额，负数为减价")
    private BigDecimal adjustAmount;
}
