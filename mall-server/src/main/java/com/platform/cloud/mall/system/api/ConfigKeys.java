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

    // ------------------------------------------------------------------ 支付与微信（V6__pay.sql）
    // 敏感键（私钥、密钥）在 sys_config.sensitive = 1，后台列表只返回掩码，见 docs/mall/payment.md

    /** 支付回调根地址，如 https://shop.example.com/api；渠道异步通知 = 根地址 + /app/public/pay/notify/{channel} */
    public static final String PAY_NOTIFY_BASE_URL = "pay.notify_base_url";
    /** 支付完成后跳回的商城页面，{orderId} 会被替换为订单 ID（支付宝 return_url / quit_url 使用） */
    public static final String PAY_RETURN_URL = "pay.return_url";

    /** 微信支付 JSAPI（公众号内）开关 true / false */
    public static final String WECHAT_PAY_ENABLED = "pay.wechat.enabled";
    /** 微信支付 H5（手机浏览器）开关；H5 支付需在商户平台单独申请开通 */
    public static final String WECHAT_PAY_H5_ENABLED = "pay.wechat.h5_enabled";
    /** 微信支付关联的公众号 AppID；留空则使用微信登录的公众号 AppID */
    public static final String WECHAT_PAY_APP_ID = "pay.wechat.app_id";
    /** 微信支付商户号 */
    public static final String WECHAT_PAY_MCH_ID = "pay.wechat.mch_id";
    /** 商户 API 证书序列号 */
    public static final String WECHAT_PAY_MCH_SERIAL_NO = "pay.wechat.mch_serial_no";
    /** APIv3 密钥（敏感） */
    public static final String WECHAT_PAY_API_V3_KEY = "pay.wechat.api_v3_key";
    /** 商户 API 私钥 apiclient_key.pem 全文（敏感） */
    public static final String WECHAT_PAY_PRIVATE_KEY = "pay.wechat.private_key";
    /** 微信支付公钥 ID（2024 年后新商户使用「微信支付公钥」验签；留空则自动下载平台证书） */
    public static final String WECHAT_PAY_PUBLIC_KEY_ID = "pay.wechat.public_key_id";
    /** 微信支付公钥 pub_key.pem 全文 */
    public static final String WECHAT_PAY_PUBLIC_KEY = "pay.wechat.public_key";

    /** 支付宝手机网站支付开关 true / false */
    public static final String ALIPAY_ENABLED = "pay.alipay.enabled";
    /** 支付宝应用 AppID */
    public static final String ALIPAY_APP_ID = "pay.alipay.app_id";
    /** 应用私钥（敏感，PKCS8 格式） */
    public static final String ALIPAY_PRIVATE_KEY = "pay.alipay.private_key";
    /** 支付宝公钥（注意不是应用公钥） */
    public static final String ALIPAY_PUBLIC_KEY = "pay.alipay.public_key";
    /** 支付宝网关，正式 https://openapi.alipay.com/gateway.do，沙箱 https://openapi-sandbox.dl.alipaydev.com/gateway.do */
    public static final String ALIPAY_GATEWAY = "pay.alipay.gateway";

    /** 微信公众号 AppID（网页授权登录） */
    public static final String WECHAT_MP_APP_ID = "wechat.mp.app_id";
    /** 微信公众号 AppSecret（敏感） */
    public static final String WECHAT_MP_APP_SECRET = "wechat.mp.app_secret";

    private ConfigKeys() {
    }
}
