package com.platform.cloud.mall.trade.order.mapper.row;

import lombok.Data;

import java.math.BigDecimal;

/** 统计查询结果：销售额与订单数 */
@Data
public class SalesRow {
    private BigDecimal sales;
    private Long orders;
}
