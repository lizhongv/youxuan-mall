package com.platform.cloud.mall.marketing.support;

/**
 * 优惠券有效期类型。
 */
public enum CouponValidType {
    /** 固定时间段：用模板的 startAt ~ endAt */
    FIXED,
    /** 领取后 N 天内有效 */
    DAYS;

    public static final String PATTERN = "FIXED|DAYS";
}
