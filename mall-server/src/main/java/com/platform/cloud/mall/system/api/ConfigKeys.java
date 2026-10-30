package com.platform.cloud.mall.system.api;

/**
 * sys_config 中已约定的键。新增键要同时在 Flyway 种子数据里插入默认值。
 */
public final class ConfigKeys {

    /** 商城名称 */
    public static final String MALL_NAME = "mall.name";
    /** 客服电话 */
    public static final String MALL_SERVICE_PHONE = "mall.service_phone";
    /** 基础运费（元） */
    public static final String FREIGHT_AMOUNT = "trade.freight_amount";
    /** 满多少元包邮（元），0 表示一律包邮 */
    public static final String FREIGHT_FREE_THRESHOLD = "trade.freight_free_threshold";
    /** 未支付自动关单（分钟） */
    public static final String ORDER_PAY_TIMEOUT_MINUTES = "trade.pay_timeout_minutes";
    /** 发货后自动确认收货（天） */
    public static final String ORDER_AUTO_RECEIVE_DAYS = "trade.auto_receive_days";
    /** 确认收货后可申请售后（天） */
    public static final String AFTERSALE_DAYS = "trade.aftersale_days";
    /** 每消费 1 元赠送积分 */
    public static final String POINTS_PER_YUAN = "member.points_per_yuan";
    /** 每消费 1 元增加成长值 */
    public static final String GROWTH_PER_YUAN = "member.growth_per_yuan";
    /** 新会员注册赠送余额（元，演示/拉新用） */
    public static final String REGISTER_GIFT_BALANCE = "member.register_gift_balance";

    private ConfigKeys() {
    }
}
