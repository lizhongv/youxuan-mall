package com.platform.cloud.mall.stats.vo;

import java.math.BigDecimal;

/** 商品销量排行 */
public record TopGoodsVO(Long goodsId, String goodsName, String pic, long quantity, BigDecimal amount) {
}
