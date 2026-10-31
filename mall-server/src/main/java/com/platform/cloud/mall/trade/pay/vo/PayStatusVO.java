package com.platform.cloud.mall.trade.pay.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 支付结果轮询 */
@Data
public class PayStatusVO {

    private Long orderId;
    /** 订单状态，PENDING_PAY 表示仍未支付 */
    private String status;
    private String statusText;
    private Boolean paid;
    private String payType;
    private LocalDateTime payTime;
}
