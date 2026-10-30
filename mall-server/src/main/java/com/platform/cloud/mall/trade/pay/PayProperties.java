package com.platform.cloud.mall.trade.pay;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付配置（mall.pay.*）。
 */
@Data
@ConfigurationProperties(prefix = "mall.pay")
public class PayProperties {

    /**
     * 是否开放模拟支付。默认开启以便演示与冒烟测试开箱即用；
     * 生产环境接入真实渠道后务必通过 MALL_PAY_MOCK_ENABLED=false 关闭。
     */
    private boolean mockEnabled = true;
}
