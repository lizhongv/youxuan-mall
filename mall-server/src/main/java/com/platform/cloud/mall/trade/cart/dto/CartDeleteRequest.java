package com.platform.cloud.mall.trade.cart.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** 删除购物车项 */
@Data
public class CartDeleteRequest {

    @NotEmpty(message = "请选择要删除的商品")
    private List<Long> ids;
}
