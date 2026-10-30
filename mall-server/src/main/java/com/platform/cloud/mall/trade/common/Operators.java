package com.platform.cloud.mall.trade.common;

/**
 * 订单日志 operator 字段的统一写法，便于排查时区分是谁触发的状态变化。
 */
public final class Operators {

    public static final String SYSTEM = "system";

    private Operators() {
    }

    public static String member(Long memberId) {
        return "member:" + memberId;
    }

    public static String admin(Long adminId) {
        return "admin:" + adminId;
    }
}
