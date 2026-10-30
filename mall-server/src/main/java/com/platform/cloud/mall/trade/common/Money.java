package com.platform.cloud.mall.trade.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额工具：全站金额统一两位小数、四舍五入。
 */
public final class Money {

    private Money() {
    }

    public static BigDecimal scale(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
