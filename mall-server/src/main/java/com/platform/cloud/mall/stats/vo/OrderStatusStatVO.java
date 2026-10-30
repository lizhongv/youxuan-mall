package com.platform.cloud.mall.stats.vo;

/** 订单状态分布 */
public record OrderStatusStatVO(String status, String statusText, long count) {
}
