package com.platform.cloud.mall.trade.pay.config;

import com.platform.cloud.mall.system.api.ConfigApi;
import com.platform.cloud.mall.system.api.ConfigKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 支付参数（sys_config，后台可改、不重启生效）。
 * <p>
 * 每次调用都从 ConfigApi 读取（ConfigApi 本身有本地缓存），渠道据此判断开关并在配置变化时重建 SDK 客户端。
 * 记录类型的 toString 已重写为不含密钥，防止被日志或异常信息带出。
 */
@Component
@RequiredArgsConstructor
public class PaySettings {

    /** 渠道异步通知路径（相对回调根地址），与 PayNotifyController 一致 */
    public static final String NOTIFY_WECHAT = "/app/public/pay/notify/wechat";
    public static final String NOTIFY_WECHAT_REFUND = "/app/public/pay/notify/wechat-refund";
    public static final String NOTIFY_ALIPAY = "/app/public/pay/notify/alipay";
    static final String DEFAULT_ALIPAY_GATEWAY = "https://openapi.alipay.com/gateway.do";

    private final ConfigApi configApi;

    public WechatPay wechat() {
        String appId = get(ConfigKeys.WECHAT_PAY_APP_ID);
        if (appId.isEmpty()) {
            // 支付与网页授权必须是同一个公众号（openid 按公众号区分），留空时直接复用登录的 AppID，少填一处
            appId = get(ConfigKeys.WECHAT_MP_APP_ID);
        }
        return new WechatPay(bool(ConfigKeys.WECHAT_PAY_ENABLED), bool(ConfigKeys.WECHAT_PAY_H5_ENABLED), appId,
                get(ConfigKeys.WECHAT_PAY_MCH_ID), get(ConfigKeys.WECHAT_PAY_MCH_SERIAL_NO),
                get(ConfigKeys.WECHAT_PAY_API_V3_KEY), get(ConfigKeys.WECHAT_PAY_PRIVATE_KEY),
                get(ConfigKeys.WECHAT_PAY_PUBLIC_KEY_ID), get(ConfigKeys.WECHAT_PAY_PUBLIC_KEY));
    }

    public Alipay alipay() {
        String gateway = get(ConfigKeys.ALIPAY_GATEWAY);
        return new Alipay(bool(ConfigKeys.ALIPAY_ENABLED), get(ConfigKeys.ALIPAY_APP_ID),
                get(ConfigKeys.ALIPAY_PRIVATE_KEY), get(ConfigKeys.ALIPAY_PUBLIC_KEY),
                gateway.isEmpty() ? DEFAULT_ALIPAY_GATEWAY : gateway);
    }

    /**
     * 完整回调地址；根地址未配置或不是 HTTPS 返回 null（微信要求 HTTPS，支付宝也建议 HTTPS），渠道视为配置不完整。
     */
    public String notifyUrl(String path) {
        String base = get(ConfigKeys.PAY_NOTIFY_BASE_URL);
        if (!base.startsWith("https://")) {
            return null;
        }
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + path;
    }

    /** 支付完成跳转页，未配置返回 null */
    public String returnUrl(Long orderId) {
        String tpl = get(ConfigKeys.PAY_RETURN_URL);
        return tpl.isEmpty() ? null : tpl.replace("{orderId}", String.valueOf(orderId));
    }

    public String mallName() {
        return get(ConfigKeys.MALL_NAME);
    }

    private String get(String key) {
        String v = configApi.getString(key, "");
        return v == null ? "" : v.trim();
    }

    private boolean bool(String key) {
        return "true".equalsIgnoreCase(get(key));
    }

    /**
     * 微信支付配置。publicKeyId + publicKey 同时填写时使用「微信支付公钥」验签（2024 年后新商户默认），
     * 否则使用平台证书模式（SDK 自动下载并定时更新平台证书）。
     */
    public record WechatPay(boolean jsapiEnabled, boolean h5Enabled, String appId, String mchId, String mchSerialNo,
                            String apiV3Key, String privateKey, String publicKeyId, String publicKey) {

        /** 缺失的必填项名称，完整时返回 null */
        public String missing() {
            if (appId.isEmpty()) {
                return "公众号AppID";
            }
            if (mchId.isEmpty()) {
                return "商户号";
            }
            if (mchSerialNo.isEmpty()) {
                return "商户证书序列号";
            }
            if (apiV3Key.isEmpty()) {
                return "APIv3密钥";
            }
            if (privateKey.isEmpty()) {
                return "商户私钥";
            }
            return null;
        }

        public boolean usePublicKey() {
            return !publicKeyId.isEmpty() && !publicKey.isEmpty();
        }

        @Override
        public String toString() {
            return "WechatPay[appId=" + appId + ", mchId=" + mchId + ", serialNo=" + mchSerialNo
                    + ", publicKeyId=" + publicKeyId + ", apiV3Key=******, privateKey=******]";
        }
    }

    /** 支付宝配置（公钥模式；证书模式仅资金转出类接口强制要求，手机网站支付与退款不需要） */
    public record Alipay(boolean enabled, String appId, String privateKey, String alipayPublicKey, String gateway) {

        public String missing() {
            if (appId.isEmpty()) {
                return "应用AppID";
            }
            if (privateKey.isEmpty()) {
                return "应用私钥";
            }
            if (alipayPublicKey.isEmpty()) {
                return "支付宝公钥";
            }
            return null;
        }

        @Override
        public String toString() {
            return "Alipay[appId=" + appId + ", gateway=" + gateway + ", privateKey=******]";
        }
    }
}
