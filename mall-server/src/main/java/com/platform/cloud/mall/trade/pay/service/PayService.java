package com.platform.cloud.mall.trade.pay.service;

import com.platform.cloud.mall.trade.order.entity.Order;

import java.math.BigDecimal;

/** 支付与退款 */
public interface PayService {

    /** 会员支付自己的待付款订单；同步渠道支付成功返回 true */
    boolean pay(Long memberId, Long orderId, String payType);

    /**
     * 渠道异步通知入口（微信 / 支付宝回调验签后调用）：按支付单号落账，重复通知幂等返回。
     */
    void onPaid(String payNo, String channelNo);

    /** 按订单原支付渠道退款 */
    void refund(Order order, BigDecimal amount, String refundNo);

    /** 整单退完后把支付流水标记为已退款，便于对账 */
    void markRefunded(Long orderId);
}
