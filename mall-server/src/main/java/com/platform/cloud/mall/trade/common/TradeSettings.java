package com.platform.cloud.mall.trade.common;

import com.platform.cloud.mall.system.api.ConfigApi;
import com.platform.cloud.mall.system.api.ConfigKeys;
import com.platform.cloud.mall.trade.order.service.PriceCalculator.FreightRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 交易运营参数的统一读取入口：值来自 sys_config（后台可改，ConfigApi 自带缓存），缺失时用 {@link TradeDefaults}。
 * 集中在这里是为了让各业务方法不重复写键名和默认值。
 */
@Component
@RequiredArgsConstructor
public class TradeSettings {

    private final ConfigApi configApi;

    public FreightRule freightRule() {
        return new FreightRule(
                configApi.getDecimal(ConfigKeys.FREIGHT_AMOUNT, TradeDefaults.FREIGHT_AMOUNT),
                configApi.getDecimal(ConfigKeys.FREIGHT_FREE_THRESHOLD, TradeDefaults.FREIGHT_FREE_THRESHOLD));
    }

    public int payTimeoutMinutes() {
        return positive(configApi.getInt(ConfigKeys.ORDER_PAY_TIMEOUT_MINUTES, TradeDefaults.PAY_TIMEOUT_MINUTES),
                TradeDefaults.PAY_TIMEOUT_MINUTES);
    }

    public int autoReceiveDays() {
        return positive(configApi.getInt(ConfigKeys.ORDER_AUTO_RECEIVE_DAYS, TradeDefaults.AUTO_RECEIVE_DAYS),
                TradeDefaults.AUTO_RECEIVE_DAYS);
    }

    public int aftersaleDays() {
        int v = configApi.getInt(ConfigKeys.AFTERSALE_DAYS, TradeDefaults.AFTERSALE_DAYS);
        // 0 表示收货后不再受理售后，是合法的运营选择
        return Math.max(v, 0);
    }

    public BigDecimal pointsPerYuan() {
        return nonNegative(configApi.getDecimal(ConfigKeys.POINTS_PER_YUAN, TradeDefaults.POINTS_PER_YUAN));
    }

    public BigDecimal growthPerYuan() {
        return nonNegative(configApi.getDecimal(ConfigKeys.GROWTH_PER_YUAN, TradeDefaults.GROWTH_PER_YUAN));
    }

    /** 超时类参数配成 0 或负数会导致订单刚创建就被关闭，按默认值处理 */
    private static int positive(int v, int def) {
        return v > 0 ? v : def;
    }

    private static BigDecimal nonNegative(BigDecimal v) {
        return v == null || v.signum() < 0 ? BigDecimal.ZERO : v;
    }
}
