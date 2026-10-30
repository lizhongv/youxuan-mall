package com.platform.cloud.mall.trade.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 修改购物车数量 */
@Data
public class CartUpdateRequest {

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 99, message = "单个商品最多 99 件")
    private Integer quantity;
}
