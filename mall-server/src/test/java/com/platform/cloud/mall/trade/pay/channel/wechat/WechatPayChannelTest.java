package com.platform.cloud.mall.trade.pay.channel.wechat;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.trade.MpTestSupport;
import com.platform.cloud.mall.trade.pay.PayType;
import com.platform.cloud.mall.trade.pay.channel.AsyncPayChannel.PrepayCommand;
import com.platform.cloud.mall.trade.pay.channel.ChannelTrade;
import com.platform.cloud.mall.trade.pay.channel.PayChannel.RefundCommand;
import com.platform.cloud.mall.trade.pay.channel.PayChannel.RefundStatus;
import com.platform.cloud.mall.trade.pay.channel.wechat.WechatPayChannel.NotifyHeaders;
import com.platform.cloud.mall.trade.pay.config.PaySettings;
import com.platform.cloud.mall.trade.pay.entity.Payment;
import com.platform.cloud.mall.trade.pay.mapper.PaymentMapper;
import com.platform.cloud.mall.trade.pay.support.PayNotifyException;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.TransactionAmount;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.Status;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 微信支付渠道：下单参数、可用性、回调验签与核对、退款状态映射（mock SDK 客户端）。
 */
class WechatPayChannelTest {

    PaySettings settings = mock(PaySettings.class);
    PaymentMapper paymentMapper = mock(PaymentMapper.class);
    WechatPayClient client = mock(WechatPayClient.class);
    AtomicInteger builds = new AtomicInteger();
    WechatPayChannel channel;

    @BeforeAll
    static void initMp() {
        MpTestSupport.init(Payment.class);
    }

    @BeforeEach
    void setUp() {
        configure(true, true, "wx-app");
        when(settings.notifyUrl(PaySettings.NOTIFY_WECHAT)).thenReturn("https://shop.example.com/api/app/public/pay/notify/wechat");
        when(settings.notifyUrl(PaySettings.NOTIFY_WECHAT_REFUND))
                .thenReturn("https://shop.example.com/api/app/public/pay/notify/wechat-refund");
        channel = new WechatPayChannel(settings, paymentMapper, s -> {
            builds.incrementAndGet();
            return client;
        });
    }

    private void configure(boolean jsapi, boolean h5, String appId) {
        when(settings.wechat()).thenReturn(new PaySettings.WechatPay(jsapi, h5, appId, "1900000001", "SERIAL",
                "apiv3key-32-bytes-xxxxxxxxxxxxxx", "-----BEGIN PRIVATE KEY-----x", "", ""));
    }

    private static PrepayCommand cmd(PayType type) {
        return new PrepayCommand(type, "P1", "NO1", new BigDecimal("88.80"), "优选商城 订单 NO1",
                LocalDateTime.of(2026, 10, 31, 12, 30, 0), "openid-1", "1.2.3.4", null);
    }

    @Test
    void JSAPI下单_参数正确且返回前端调起参数() {
        PrepayWithRequestPaymentResponse r = new PrepayWithRequestPaymentResponse();
        r.setAppId("wx-app");
        r.setTimeStamp("1700000000");
        r.setNonceStr("n");
        r.setPackageVal("prepay_id=wx1");
        r.setSignType("RSA");
        r.setPaySign("sig");
        when(client.jsapiPrepay(any())).thenReturn(r);

        Map<String, String> params = channel.prepay(cmd(PayType.WECHAT_JSAPI));

        ArgumentCaptor<PrepayRequest> req = ArgumentCaptor.forClass(PrepayRequest.class);
        verify(client).jsapiPrepay(req.capture());
        assertThat(req.getValue().getAppid()).isEqualTo("wx-app");
        assertThat(req.getValue().getMchid()).isEqualTo("1900000001");
        assertThat(req.getValue().getOutTradeNo()).isEqualTo("P1");
        assertThat(req.getValue().getAmount().getTotal()).isEqualTo(8880);
        assertThat(req.getValue().getAmount().getCurrency()).isEqualTo("CNY");
        assertThat(req.getValue().getPayer().getOpenid()).isEqualTo("openid-1");
        assertThat(req.getValue().getTimeExpire()).isEqualTo("2026-10-31T12:30:00+08:00");
        assertThat(req.getValue().getNotifyUrl()).endsWith("/app/public/pay/notify/wechat");
        assertThat(params).containsEntry("package", "prepay_id=wx1").containsEntry("paySign", "sig")
                .containsKeys("appId", "timeStamp", "nonceStr", "signType");
    }

    @Test
    void H5下单_上送用户IP与Wap场景_返回h5Url() {
        when(client.h5Prepay(any())).thenReturn("https://wx.tenpay.com/h5?prepay_id=1");

        Map<String, String> params = channel.prepay(cmd(PayType.WECHAT_H5));

        ArgumentCaptor<com.wechat.pay.java.service.payments.h5.model.PrepayRequest> req =
                ArgumentCaptor.forClass(com.wechat.pay.java.service.payments.h5.model.PrepayRequest.class);
        verify(client).h5Prepay(req.capture());
        assertThat(req.getValue().getSceneInfo().getPayerClientIp()).isEqualTo("1.2.3.4");
        assertThat(req.getValue().getSceneInfo().getH5Info().getType()).isEqualTo("Wap");
        assertThat(params).containsEntry("h5Url", "https://wx.tenpay.com/h5?prepay_id=1");
    }

    @Test
    void 可用性_开关关闭_配置不全_回调非HTTPS() {
        configure(false, true, "wx-app");
        assertThat(channel.availability(PayType.WECHAT_JSAPI).on()).isFalse();
        assertThat(channel.availability(PayType.WECHAT_H5).ready()).isTrue();

        configure(true, true, "");
        assertThat(channel.availability(PayType.WECHAT_JSAPI).ready()).isFalse();

        configure(true, true, "wx-app");
        when(settings.notifyUrl(PaySettings.NOTIFY_WECHAT)).thenReturn(null);
        assertThat(channel.availability(PayType.WECHAT_JSAPI).ready()).isFalse();
    }

    @Test
    void 配置不变时复用客户端_配置变化重建() {
        channel.availability(PayType.WECHAT_JSAPI);
        channel.availability(PayType.WECHAT_JSAPI);
        assertThat(builds.get()).isEqualTo(1);
        configure(true, true, "wx-app-2");
        channel.availability(PayType.WECHAT_JSAPI);
        assertThat(builds.get()).isEqualTo(2);
    }

    @Test
    void SDK构建失败_渠道不可用() {
        WechatPayChannel broken = new WechatPayChannel(settings, paymentMapper, s -> {
            throw new IllegalArgumentException("私钥格式错误");
        });
        assertThat(broken.availability(PayType.WECHAT_JSAPI).ready()).isFalse();
        assertThatThrownBy(() -> broken.prepay(cmd(PayType.WECHAT_JSAPI))).isInstanceOf(BusinessException.class);
    }

    private static Transaction tx(String mchId, String appId, Transaction.TradeStateEnum state, int total) {
        Transaction t = new Transaction();
        t.setMchid(mchId);
        t.setAppid(appId);
        t.setOutTradeNo("P1");
        t.setTransactionId("4200001");
        t.setTradeState(state);
        TransactionAmount a = new TransactionAmount();
        a.setTotal(total);
        t.setAmount(a);
        return t;
    }

    private static NotifyHeaders headers() {
        return new NotifyHeaders("SERIAL", "nonce", "sig", "1700000000", "WECHATPAY2-SHA256-RSA2048");
    }

    @Test
    void 回调验签通过_转换为渠道交易() {
        when(client.parseNotification(any(RequestParam.class), eq(Transaction.class)))
                .thenReturn(tx("1900000001", "wx-app", Transaction.TradeStateEnum.SUCCESS, 8880));

        ChannelTrade t = channel.parsePayNotify(headers(), "{}");

        assertThat(t.paid()).isTrue();
        assertThat(t.payNo()).isEqualTo("P1");
        assertThat(t.amountFen()).isEqualTo(8880);
        assertThat(t.channelNo()).isEqualTo("4200001");
    }

    @Test
    void 回调验签失败_拒绝() {
        when(client.parseNotification(any(RequestParam.class), eq(Transaction.class)))
                .thenThrow(new ValidationException("签名错误"));
        assertThatThrownBy(() -> channel.parsePayNotify(headers(), "{}")).isInstanceOf(PayNotifyException.class);
    }

    @Test
    void 回调商户号不符_拒绝() {
        when(client.parseNotification(any(RequestParam.class), eq(Transaction.class)))
                .thenReturn(tx("other", "wx-app", Transaction.TradeStateEnum.SUCCESS, 8880));
        assertThatThrownBy(() -> channel.parsePayNotify(headers(), "{}")).isInstanceOf(PayNotifyException.class);
    }

    @Test
    void 退款_金额按分_总额取支付流水_处理中() {
        Payment p = new Payment();
        p.setPayNo("P1");
        p.setAmount(new BigDecimal("88.80"));
        when(paymentMapper.selectOne(any())).thenReturn(p);
        Refund r = new Refund();
        r.setStatus(Status.PROCESSING);
        when(client.createRefund(any())).thenReturn(r);

        var result = channel.refund(new RefundCommand(1L, 2L, "NO1", "P1", "A1", new BigDecimal("10.00")));

        ArgumentCaptor<CreateRequest> req = ArgumentCaptor.forClass(CreateRequest.class);
        verify(client).createRefund(req.capture());
        assertThat(req.getValue().getOutRefundNo()).isEqualTo("A1");
        assertThat(req.getValue().getAmount().getRefund()).isEqualTo(1000L);
        assertThat(req.getValue().getAmount().getTotal()).isEqualTo(8880L);
        assertThat(req.getValue().getNotifyUrl()).endsWith("/wechat-refund");
        assertThat(result.status()).isEqualTo(RefundStatus.PROCESSING);
    }

    @Test
    void 退款状态与交易状态映射() {
        assertThat(WechatPayChannel.toRefundResult(Status.SUCCESS).status()).isEqualTo(RefundStatus.SUCCESS);
        assertThat(WechatPayChannel.toRefundResult(Status.ABNORMAL).status()).isEqualTo(RefundStatus.FAILED);
        assertThat(WechatPayChannel.toTrade(tx("m", "a", Transaction.TradeStateEnum.NOTPAY, 1), "P1").state())
                .isEqualTo(ChannelTrade.State.WAITING);
        assertThat(WechatPayChannel.toTrade(tx("m", "a", Transaction.TradeStateEnum.CLOSED, 1), "P1").state())
                .isEqualTo(ChannelTrade.State.CLOSED);
    }

    @Test
    void 商品描述按UTF8字节截断() {
        String s = "商".repeat(60);
        String t = WechatPayChannel.truncateBytes(s, 127);
        assertThat(t.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isLessThanOrEqualTo(127);
        assertThat(t).hasSize(42);
    }
}
