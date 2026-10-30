package com.platform.cloud.mall.trade.common;

import java.math.BigDecimal;

/**
 * 交易参数的兜底默认值。
 * <p>
 * 正式值在 sys_config 中由运营调整（见 ConfigKeys），这里只在配置缺失时使用，保证新库也能跑通下单链路。
 */
public final class TradeDefaults {

    /** 基础运费 10 元：国内快递首重常见价位 */
    public static final BigDecimal FREIGHT_AMOUNT = new BigDecimal("10.00");
    /** 满 99 包邮：主流电商常用门槛 */
    public static final BigDecimal FREIGHT_FREE_THRESHOLD = new BigDecimal("99.00");
    /** 30 分钟未支付关单：兼顾用户犹豫时间与库存占用 */
    public static final int PAY_TIMEOUT_MINUTES = 30;
    /** 发货 7 天后自动确认收货：覆盖绝大多数快递时效 */
    public static final int AUTO_RECEIVE_DAYS = 7;
    /** 确认收货后 7 天内可申请售后：与「七天无理由」一致 */
    public static final int AFTERSALE_DAYS = 7;
    /** 每消费 1 元送 1 积分 / 1 成长值 */
    public static final BigDecimal POINTS_PER_YUAN = BigDecimal.ONE;
    public static final BigDecimal GROWTH_PER_YUAN = BigDecimal.ONE;
    /** 单个商品购买 / 加购上限：防止误操作和恶意囤货 */
    public static final int MAX_QUANTITY = 99;
    /** 应付金额下限：支付渠道不接受 0 元订单 */
    public static final BigDecimal MIN_PAY_AMOUNT = new BigDecimal("0.01");

    private TradeDefaults() {
    }
}
