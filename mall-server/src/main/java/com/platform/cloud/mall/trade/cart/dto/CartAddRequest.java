package com.platform.cloud.mall.trade.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 加入购物车 */
@Data
public class CartAddRequest {

    @NotNull(message = "skuId 不能为空")
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 99, message = "单个商品最多 99 件")
    @Schema(description = "数量；已在购物车中则累加")
    private Integer quantity;
}
