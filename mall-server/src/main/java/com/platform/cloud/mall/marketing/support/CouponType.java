package com.platform.cloud.mall.marketing.support;

/**
 * 优惠券类型。
 */
public enum CouponType {
    /** 满减：适用金额 ≥ 门槛时减 amount */
    FULL_REDUCTION,
    /** 折扣：适用金额 × (1 - discount)，maxAmount &gt; 0 时封顶 */
    DISCOUNT,
    /** 无门槛：直接减 amount */
    NO_THRESHOLD;

    public static final String PATTERN = "FULL_REDUCTION|DISCOUNT|NO_THRESHOLD";
}
