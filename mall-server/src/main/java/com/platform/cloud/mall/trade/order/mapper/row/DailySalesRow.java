package com.platform.cloud.mall.trade.order.mapper.row;

import lombok.Data;

import java.math.BigDecimal;

/** 统计查询结果：按天的销售额与订单数，date 为 yyyy-MM-dd */
@Data
public class DailySalesRow {
    private String date;
    private BigDecimal sales;
    private Long orders;
}
