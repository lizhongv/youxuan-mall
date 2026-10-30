package com.platform.cloud.mall.trade.order.mapper.row;

import lombok.Data;

import java.math.BigDecimal;

/** 统计查询结果：商品销量排行 */
@Data
public class GoodsSalesRow {
    private Long goodsId;
    private String goodsName;
    private String pic;
    private Long quantity;
    private BigDecimal amount;
}
