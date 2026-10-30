package com.platform.cloud.mall.trade.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 取消订单入参 */
@Data
public class OrderCancelRequest {

    @Size(max = 200, message = "取消原因不能超过 200 字")
    @Schema(description = "取消原因（字典 cancel_reason）")
    private String reason;
}
