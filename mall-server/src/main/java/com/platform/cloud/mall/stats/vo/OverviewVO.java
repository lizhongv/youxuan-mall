package com.platform.cloud.mall.stats.vo;

import java.math.BigDecimal;

/** 数据概览 */
public record OverviewVO(BigDecimal todaySales, long todayOrders, long todayMembers,
                         BigDecimal yesterdaySales, long yesterdayOrders,
                         long totalMembers, long onSaleGoods,
                         long pendingDelivery, long pendingAftersale, long pendingPay) {
}
