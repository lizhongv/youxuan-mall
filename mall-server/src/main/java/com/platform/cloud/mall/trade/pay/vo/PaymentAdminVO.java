package com.platform.cloud.mall.trade.pay.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 后台支付流水 */
@Data
public class PaymentAdminVO {

    private Long id;
    private String payNo;
    private Long orderId;
    private String orderNo;
    private Long memberId;
    private String channel;
    private String channelText;
    private String tradeType;
    private BigDecimal amount;
    private String status;
    private String statusText;
    private String channelNo;
    private LocalDateTime paidAt;
    private String remark;
    private LocalDateTime createdAt;
}
