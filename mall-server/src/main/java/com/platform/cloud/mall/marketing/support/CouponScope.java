package com.platform.cloud.mall.marketing.support;

/**
 * 优惠券适用范围。CATEGORY 的 scopeIds 可以是一级或二级类目，一级包含其下全部二级类目。
 */
public enum CouponScope {
    ALL,
    CATEGORY,
    GOODS;

    public static final String PATTERN = "ALL|CATEGORY|GOODS";
}
