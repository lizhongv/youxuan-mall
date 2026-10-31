package com.platform.cloud.mall.trade.pay.channel;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付渠道抽象。每个渠道一个实现，按 {@link #type()} 注册，由 PayService 路由。
 * <p>
 * 同步渠道（余额、模拟）在 {@link #pay} 内完成扣款并返回 success=true；
 * 异步渠道（微信、支付宝）在 {@link #pay} 内调用统一下单，返回 success=false 与客户端调起参数 payParams，
 * 真正的支付结果由渠道回调 → 验签 → {@code PaySettlementService.settle} → {@code PayService.onPaid} 落账。
 * 异步渠道另实现 {@link AsyncPayChannel}（下单、查单、关单、查退款）。
 */
public interface PayChannel {

    /** 渠道编码，与 trd_order.pay_type、trd_payment.channel 一致：BALANCE / MOCK / WECHAT / ALIPAY（见 PayType） */
    String type();

    /** 渠道是否可用（如模拟支付在生产环境关闭） */
    default boolean enabled() {
        return true;
    }

    PayResult pay(PayCommand cmd);

    /**
     * 原路退款；请求失败抛异常让售后事务回滚，避免「退款单已完成但钱没退」。
     * 渠道受理但未完成（微信退款通常异步到账）返回 PROCESSING，由退款回调 / 补偿任务落定结果。
     */
    RefundResult refund(RefundCommand cmd);

    record PayCommand(Long memberId, Long orderId, String orderNo, String payNo, BigDecimal amount) {
    }

    /**
     * @param success   是否已同步支付成功
     * @param channelNo 渠道交易号
     * @param payParams 异步渠道给客户端的调起参数
     */
    record PayResult(boolean success, String channelNo, Map<String, String> payParams) {

        public static PayResult paid(String channelNo) {
            return new PayResult(true, channelNo, Map.of());
        }
    }

    /**
     * @param refundNo 退款单号（售后单号），渠道侧用于退款幂等
     */
    record RefundCommand(Long memberId, Long orderId, String orderNo, String payNo, String refundNo,
                         BigDecimal amount) {
    }

    /** 退款结果状态 */
    enum RefundStatus {
        /** 已退回 */
        SUCCESS,
        /** 渠道已受理，等待回调或查询 */
        PROCESSING,
        /** 渠道退款失败（如微信 ABNORMAL：用户银行卡注销），需商户平台人工处理 */
        FAILED,
        /** 不经渠道，需线下退款（模拟支付） */
        OFFLINE
    }

    /** @param message 失败原因或线下处理说明，写入售后单备注 */
    record RefundResult(RefundStatus status, String message) {

        public static RefundResult success() {
            return new RefundResult(RefundStatus.SUCCESS, "");
        }

        public static RefundResult processing() {
            return new RefundResult(RefundStatus.PROCESSING, "");
        }

        public static RefundResult failed(String message) {
            return new RefundResult(RefundStatus.FAILED, message == null ? "" : message);
        }

        public static RefundResult offline(String message) {
            return new RefundResult(RefundStatus.OFFLINE, message);
        }
    }
}
