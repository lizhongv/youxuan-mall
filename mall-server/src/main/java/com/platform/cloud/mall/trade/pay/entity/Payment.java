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

    private String payNo;
    private Long orderId;
    private Long memberId;
    private String channel;
    private BigDecimal amount;
    private String status;
    private String channelNo;
    private LocalDateTime paidAt;
}
