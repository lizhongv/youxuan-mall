package com.platform.cloud.mall.marketing.support;

/**
 * 会员券状态。UNUSED → USED（下单核销）→ UNUSED / EXPIRED（订单取消退还）；UNUSED → EXPIRED（定时任务）。
 */
public enum MemberCouponStatus {
    UNUSED,
    USED,
    EXPIRED;

    public static final String PATTERN = "UNUSED|USED|EXPIRED";
}
