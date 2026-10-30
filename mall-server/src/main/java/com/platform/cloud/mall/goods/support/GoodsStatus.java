package com.platform.cloud.mall.goods.support;

/**
 * 商品上下架状态，库里存枚举名，便于直接看数据排查。
 */
public enum GoodsStatus {
    ON_SALE,
    OFF_SALE;

    /** 入参校验用的正则，与枚举保持一致 */
    public static final String PATTERN = "ON_SALE|OFF_SALE";
}
