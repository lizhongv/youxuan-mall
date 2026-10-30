package com.platform.cloud.mall.trade.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 订单明细 VO */
@Data
public class OrderItemVO {

    private Long id;
    private Long goodsId;
    private Long skuId;
    private String goodsName;
    private String pic;
    private String specText;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private String aftersaleStatus;
    private Integer reviewed;
}
