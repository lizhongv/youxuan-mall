package com.platform.cloud.mall.trade.pay.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 支付流水（trd_payment）。一次支付尝试一条，对账以此为准。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trd_payment")
public class Payment extends BaseEntity {

    public static final String WAITING = "WAITING";
    public static final String SUCCESS = "SUCCESS";
    public static final String CLOSED = "CLOSED";
    public static final String REFUNDED = "REFUNDED";
    /** 订单已关闭（或已由其他支付单付清、金额已变更）后才到账，系统已自动原路退款 */
    public static final String REFUNDED_AFTER_CLOSE = "REFUNDED_AFTER_CLOSE";

    private String payNo;
    private Long orderId;
    private Long memberId;
    private String channel;
    /** 发起方式（PayType 名），同步渠道为空串 */
    private String tradeType;
    private BigDecimal amount;
    private String status;
    private String channelNo;
    /** 返回给前端的调起参数 JSON，短时间内重复发起时复用 */
    private String payParams;
    private LocalDateTime paidAt;
    /** 关闭原因、异常说明 */
    private String remark;
}
