package com.platform.cloud.mall.trade.pay.channel;

import com.platform.cloud.mall.trade.pay.PayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 模拟支付：立即成功，用于演示与冒烟测试。
 * <p>
 * 生产环境接入真实渠道后应设置 {@code mall.pay.mock-enabled=false}，否则任何人都能「零元购」。
 * 退款视为线下退款，只记录日志。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockPayChannel implements PayChannel {

    public static final String TYPE = "MOCK";

    private final PayProperties payProperties;

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public boolean enabled() {
        return payProperties.isMockEnabled();
    }

    @Override
    public PayResult pay(PayCommand cmd) {
        return PayResult.paid("MOCK" + cmd.payNo());
    }

    @Override
    public void refund(RefundCommand cmd) {
        log.info("模拟支付订单退款（线下处理）orderNo={} refundNo={} amount={}",
                cmd.orderNo(), cmd.refundNo(), cmd.amount());
    }
}
