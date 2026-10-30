package com.platform.cloud.mall.trade.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 商家备注 */
@Data
public class OrderRemarkRequest {

    @NotNull(message = "备注不能为空")
    @Size(max = 255, message = "备注不能超过 255 字")
    @Schema(description = "商家备注，传空串表示清空")
    private String adminRemark;
}
