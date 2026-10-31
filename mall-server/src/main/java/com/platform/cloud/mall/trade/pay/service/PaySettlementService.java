package com.platform.cloud.mall.trade.pay.service;

import com.platform.cloud.mall.trade.pay.channel.ChannelTrade;
import com.platform.cloud.mall.trade.pay.channel.PayChannel.RefundResult;
import com.platform.cloud.mall.trade.pay.entity.Payment;

/**
 * 异步渠道的结果落账：回调、前端轮询查单、补偿任务、后台手动查单全部汇聚到这里，
 * 「支付成功」只有 {@link #settle} 一个入口，保证幂等与金额核对规则一致。
 */
public interface PaySettlementService {

    /** 退款单号前缀：关单后到账的自动退款用 R + 支付单号（售后退款用售后单号，以 A 开头） */
    String AFTER_CLOSE_REFUND_PREFIX = "R";

    /**
     * 渠道确认的交易结果落账（调用前必须已验签或来自主动查单）。
     * <ul>
     *   <li>非 PAID 状态忽略；流水已终态（成功 / 已退款）直接返回 ALREADY；</li>
     *   <li>金额（分）与流水不符抛 PayNotifyException，不落账；</li>
     *   <li>订单待付款且金额一致 → PayService.onPaid；</li>
     *   <li>订单已关闭、已被其他支付单付清或订单金额已变更 → 自动原路退款，流水标记 REFUNDED_AFTER_CLOSE。</li>
     * </ul>
     */
    SettleOutcome settle(ChannelTrade trade);

    /**
     * 主动向渠道查单并落账（补偿）：已支付 → settle；未支付且订单已不可支付 → 关闭渠道订单；渠道已关闭 → 本地关闭。
     *
     * @return 处理后的最新流水
     */
    Payment sync(Payment payment);

    /** 渠道退款结果（退款回调 / 查询）分发：售后单 → 售后领域；关单后到账退款 → 只记录 */
    void onRefundResult(String refundNo, RefundResult result);

    /** 补偿任务：查询 5 分钟前发起仍 WAITING 的异步支付流水，返回处理条数 */
    int compensateWaiting(int batch);

    /** 补偿任务：查询退款中超过 5 分钟的售后单的渠道退款结果，返回处理条数 */
    int syncRefunding(int batch);

    enum SettleOutcome {
        /** 本次落账成功，订单已置为待发货 */
        PAID,
        /** 之前已处理过（重复通知） */
        ALREADY,
        /** 关单后到账 / 重复支付，已自动原路退款 */
        REFUNDED_AFTER_CLOSE,
        /** 非支付成功的通知，无需处理 */
        IGNORED
    }
}
