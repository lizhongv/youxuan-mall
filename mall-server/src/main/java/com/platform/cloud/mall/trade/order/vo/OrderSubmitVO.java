package com.platform.cloud.mall.trade.order.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 提交订单出参 */
public record OrderSubmitVO(Long orderId, String orderNo, BigDecimal payAmount, LocalDateTime payExpireAt) {
}
