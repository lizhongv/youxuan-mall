package com.platform.cloud.mall.trade.pay;

import com.platform.cloud.bootstrap.common.exception.BusinessException;

/**
 * 前端可选的支付方式。一个渠道（channel，记在 trd_order.pay_type / trd_payment.channel）
 * 可以有多种发起方式：微信分公众号内 JSAPI 与手机浏览器 H5，二者共用商户号与回调。
 */
public enum PayType {

    BALANCE("BALANCE", true, "余额支付", "balance"),
    WECHAT_JSAPI("WECHAT", false, "微信支付", "wechat"),
    WECHAT_H5("WECHAT", false, "微信支付", "wechat"),
    ALIPAY_WAP("ALIPAY", false, "支付宝", "alipay"),
    MOCK("MOCK", true, "模拟支付", "mock");

    public static final String CHANNEL_WECHAT = "WECHAT";
    public static final String CHANNEL_ALIPAY = "ALIPAY";

    private final String channel;
    private final boolean sync;
    private final String text;
    private final String icon;

    PayType(String channel, boolean sync, String text, String icon) {
        this.channel = channel;
        this.sync = sync;
        this.text = text;
        this.icon = icon;
    }

    /** 渠道编码，路由到 PayChannel.type() */
    public String channel() {
        return channel;
    }

    /** 同步渠道在发起时就完成扣款，异步渠道需等待回调 */
    public boolean sync() {
        return sync;
    }

    public String text() {
        return text;
    }

    /** 前端收银台图标名，前端按名称映射到本地图标资源 */
    public String icon() {
        return icon;
    }

    public static PayType of(String name) {
        for (PayType t : values()) {
            if (t.name().equals(name)) {
                return t;
            }
        }
        throw new BusinessException("暂不支持该支付方式");
    }

    /** 渠道编码的展示名（后台流水列表用） */
    public static String channelText(String channel) {
        if (channel == null) {
            return "";
        }
        return switch (channel) {
            case "BALANCE" -> "余额";
            case CHANNEL_WECHAT -> "微信支付";
            case CHANNEL_ALIPAY -> "支付宝";
            case "MOCK" -> "模拟支付";
            default -> channel;
        };
    }
}
