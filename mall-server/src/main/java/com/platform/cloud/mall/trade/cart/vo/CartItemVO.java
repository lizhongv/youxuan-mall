package com.platform.cloud.mall.trade.cart.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/** 购物车项：价格、库存为实时值 */
@Data
public class CartItemVO {

    private Long id;
    private Long goodsId;
    private Long skuId;
    private String goodsName;
    private String pic;
    private String specText;
    private BigDecimal price;
    private Integer quantity;
    private Boolean checked;
    private Integer stock;
    @Schema(description = "在售且有库存")
    private Boolean valid;
}
