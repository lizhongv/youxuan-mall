package com.platform.cloud.mall.stats.vo;

import java.math.BigDecimal;

/** 趋势图的一天；date 为 yyyy-MM-dd */
public record TrendPointVO(String date, BigDecimal sales, long orders, long members) {
}
