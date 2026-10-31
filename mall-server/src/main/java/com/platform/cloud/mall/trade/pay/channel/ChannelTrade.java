package com.platform.cloud.mall.trade.pay.channel;

/**
 * 渠道侧的交易状态（来自验签后的回调或主动查单），统一成与渠道无关的结构交给结算逻辑。
 *
 * @param payNo     商户订单号（本系统支付单号）
 * @param channelNo 渠道交易号（微信 transaction_id / 支付宝 trade_no）
 * @param amountFen 订单总金额（分）。金额比较一律用分的整数，避免 BigDecimal 精度 / scale 差异
 */
public record ChannelTrade(String payNo, State state, String channelNo, long amountFen) {

    public enum State {
        /** 已支付 */
        PAID,
        /** 未支付 / 支付中 */
        WAITING,
        /** 已关闭 / 已撤销 / 支付失败 */
        CLOSED,
        /** 渠道没有这笔订单（支付宝在用户未打开收银台前查不到） */
        NOT_EXIST,
        /** 已支付后被退款（非本系统发起时需人工核对） */
        REFUNDED
    }

    public boolean paid() {
        return state == State.PAID;
    }
}
