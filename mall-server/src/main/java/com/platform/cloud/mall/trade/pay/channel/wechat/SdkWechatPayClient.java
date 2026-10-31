package com.platform.cloud.mall.trade.pay.channel.wechat;

import com.platform.cloud.mall.trade.pay.config.PaySettings;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.h5.H5Service;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.CloseOrderRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest;
import com.wechat.pay.java.service.refund.model.Refund;

/**
 * 基于官方 SDK 的实现。按 SDK 文档：
 * <ul>
 *   <li>配置了「微信支付公钥」时用 {@link RSAPublicKeyConfig}（2024 年 Q3 起新商户只发放公钥，不再有平台证书）；</li>
 *   <li>否则用 {@link RSAAutoCertificateConfig}：构建时用 APIv3 密钥下载平台证书并每 60 分钟自动更新。</li>
 * </ul>
 * 两种 Config 同时实现 NotificationConfig，回调验签解密复用同一份配置。
 */
public class SdkWechatPayClient implements WechatPayClient {

    private final JsapiServiceExtension jsapi;
    private final H5Service h5;
    private final RefundService refund;
    private final NotificationParser parser;
    private final String mchId;

    private SdkWechatPayClient(Config config, NotificationConfig notificationConfig, String mchId) {
        this.jsapi = new JsapiServiceExtension.Builder().config(config).build();
        this.h5 = new H5Service.Builder().config(config).build();
        this.refund = new RefundService.Builder().config(config).build();
        this.parser = new NotificationParser(notificationConfig);
        this.mchId = mchId;
    }

    /** 按后台配置构建；私钥 / 密钥错误时 SDK 抛异常，由 ConfiguredClientCache 记录并视为不可用 */
    public static SdkWechatPayClient create(PaySettings.WechatPay s) {
        if (s.usePublicKey()) {
            RSAPublicKeyConfig c = new RSAPublicKeyConfig.Builder()
                    .merchantId(s.mchId())
                    .privateKey(s.privateKey())
                    .merchantSerialNumber(s.mchSerialNo())
                    .publicKeyId(s.publicKeyId())
                    .publicKey(s.publicKey())
                    .apiV3Key(s.apiV3Key())
                    .build();
            return new SdkWechatPayClient(c, c, s.mchId());
        }
        RSAAutoCertificateConfig c = new RSAAutoCertificateConfig.Builder()
                .merchantId(s.mchId())
                .privateKey(s.privateKey())
                .merchantSerialNumber(s.mchSerialNo())
                .apiV3Key(s.apiV3Key())
                .build();
        return new SdkWechatPayClient(c, c, s.mchId());
    }

    @Override
    public PrepayWithRequestPaymentResponse jsapiPrepay(
            com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest req) {
        return jsapi.prepayWithRequestPayment(req);
    }

    @Override
    public String h5Prepay(com.wechat.pay.java.service.payments.h5.model.PrepayRequest req) {
        return h5.prepay(req).getH5Url();
    }

    @Override
    public Transaction queryByOutTradeNo(String mchId, String outTradeNo) {
        // H5 与 JSAPI 查单 / 关单是同一个 v3 接口，统一用 JSAPI 的服务对象
        QueryOrderByOutTradeNoRequest req = new QueryOrderByOutTradeNoRequest();
        req.setMchid(mchId);
        req.setOutTradeNo(outTradeNo);
        return jsapi.queryOrderByOutTradeNo(req);
    }

    @Override
    public void closeOrder(String mchId, String outTradeNo) {
        CloseOrderRequest req = new CloseOrderRequest();
        req.setMchid(mchId);
        req.setOutTradeNo(outTradeNo);
        jsapi.closeOrder(req);
    }

    @Override
    public Refund createRefund(CreateRequest req) {
        return refund.create(req);
    }

    @Override
    public Refund queryRefund(String outRefundNo) {
        QueryByOutRefundNoRequest req = new QueryByOutRefundNoRequest();
        req.setOutRefundNo(outRefundNo);
        return refund.queryByOutRefundNo(req);
    }

    @Override
    public <T> T parseNotification(RequestParam param, Class<T> clazz) {
        return parser.parse(param, clazz);
    }

    @Override
    public String toString() {
        return "SdkWechatPayClient[mchId=" + mchId + "]";
    }
}
