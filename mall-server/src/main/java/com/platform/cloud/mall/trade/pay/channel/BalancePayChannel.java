package com.platform.cloud.mall.trade.pay.channel;

import com.platform.cloud.mall.member.api.MemberApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 余额支付：直接调会员领域扣减余额并写账户流水。
 * <p>
 * 与订单状态更新处于同一数据库事务，订单状态条件更新失败（如已被关单）时扣款一起回滚，不会出现扣了钱订单却关闭。
 */
@Component
@RequiredArgsConstructor
public class BalancePayChannel implements PayChannel {

    public static final String TYPE = "BALANCE";
    /** mbr_account_log.biz_type */
    static final String BIZ_PAY = "ORDER_PAY";
    static final String BIZ_REFUND = "ORDER_REFUND";

    private final MemberApi memberApi;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public PayResult pay(PayCommand cmd) {
        // 余额不足由会员领域抛「余额不足」业务异常
        memberApi.changeBalance(cmd.memberId(), cmd.amount().negate(), BIZ_PAY, cmd.orderNo(), "订单支付");
        return PayResult.paid(cmd.payNo());
    }

    @Override
    public void refund(RefundCommand cmd) {
        memberApi.changeBalance(cmd.memberId(), cmd.amount(), BIZ_REFUND, cmd.orderNo(),
                "订单退款 " + cmd.refundNo());
    }
}
