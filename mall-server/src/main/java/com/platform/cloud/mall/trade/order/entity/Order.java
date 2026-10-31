package com.platform.cloud.mall.trade.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单（trd_order）。status 存枚举名字符串，取值与流转规则见 OrderStatus。
 * <p>
 * 状态字段只允许通过 OrderStateMachine 的条件更新修改，不要直接 updateById，否则会绕过并发控制和日志。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trd_order")
public class Order extends BaseEntity {

    private String orderNo;
    private Long memberId;
    private String orderType;
    /** 秒杀活动 ID，普通订单为空；关单归还名额、个人限购统计都以它为准 */
    private Long seckillId;
    private String status;
    private BigDecimal goodsAmount;
    private BigDecimal freightAmount;
    private BigDecimal couponAmount;
    private BigDecimal discountAmount;
    private BigDecimal adjustAmount;
    private BigDecimal payAmount;
    private Long memberCouponId;
    private String payType;
    private LocalDateTime payTime;
    private String payNo;
    private LocalDateTime payExpireAt;
    private String receiverName;
    private String receiverMobile;
    private String receiverProvince;
    private String receiverCity;
    private String receiverDistrict;
    private String receiverDetail;
    private String remark;
    private String adminRemark;
    private String deliveryCompany;
    private String deliveryNo;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
    private LocalDateTime autoReceiveAt;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private Integer reviewed;
    private Integer memberDeleted;
    /** 确认收货时实际发放的积分 / 成长值与计算基数，退款时按「退款额 / 基数」比例回收；未发放过为空 */
    private Integer rewardPoints;
    private Integer rewardGrowth;
    private BigDecimal rewardBase;
}
