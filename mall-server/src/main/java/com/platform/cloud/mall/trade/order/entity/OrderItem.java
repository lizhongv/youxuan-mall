package com.platform.cloud.mall.trade.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 订单明细（trd_order_item）。商品名、图、规格、单价都是下单时快照，商品后续改动不影响历史订单。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trd_order_item")
public class OrderItem extends BaseEntity {

    /** 明细售后状态：无 / 进行中 / 已退款 */
    public static final String AS_NONE = "NONE";
    public static final String AS_APPLYING = "APPLYING";
    public static final String AS_REFUNDED = "REFUNDED";

    private Long orderId;
    private Long memberId;
    private Long goodsId;
    private Long skuId;
    private String goodsName;
    private String pic;
    private String specText;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalAmount;
    /** 分摊折扣与优惠券后的实付，是售后退款的上限 */
    private BigDecimal payAmount;
    private String aftersaleStatus;
    private Integer reviewed;
}
