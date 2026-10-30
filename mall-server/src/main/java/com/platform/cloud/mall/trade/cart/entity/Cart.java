package com.platform.cloud.mall.trade.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 购物车项（trd_cart）。只存 SKU 与数量，价格、库存每次展示时实时取，避免价格快照过期。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trd_cart")
public class Cart extends BaseEntity {

    private Long memberId;
    private Long goodsId;
    private Long skuId;
    private Integer quantity;
    /** 1 勾选 0 未勾选 */
    private Integer checked;
}
