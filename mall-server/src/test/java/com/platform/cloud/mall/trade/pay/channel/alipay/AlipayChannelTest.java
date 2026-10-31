package com.platform.cloud.mall.trade.pay.channel.alipay;

import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.platform.cloud.mall.trade.pay.PayType;
import com.platform.cloud.mall.trade.pay.channel.AsyncPayChannel.PrepayCommand;
import com.platform.cloud.mall.trade.pay.channel.ChannelTrade;
import com.platform.cloud.mall.trade.pay.channel.PayChannel.RefundCommand;
import com.platform.cloud.mall.trade.pay.channel.PayChannel.RefundStatus;
import com.platform.cloud.mall.trade.pay.config.PaySettings;
import com.platform.cloud.mall.trade.pay.entity.Payment;
import com.platform.cloud.mall.trade.pay.support.PayNotifyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付宝渠道：手机网站支付下单、异步通知验签与核对、查单、退款（mock AlipayClient 与验签器）。
 */
class AlipayChannelTest {

    PaySettings settings = mock(PaySettings.class);
    AlipaySignatureVerifier verifier = mock(AlipaySignatureVerifier.class);
    AlipayClient client = mock(AlipayClient.class);
    AlipayChannel channel;

    @BeforeEach
    void setUp() {
        when(settings.alipay()).thenReturn(new PaySettings.Alipay(true, "2021000000", "app-private-key",
                "alipay-public-key", "https://openapi.alipay.com/gateway.do"));
        when(settings.notifyUrl(PaySettings.NOTIFY_ALIPAY))
                .thenReturn("https://shop.example.com/api/app/public/pay/notify/alipay");
        channel = new AlipayChannel(settings, verifier, s -> client);
    }

    @Test
    void 手机网站支付下单_GET方式返回跳转地址() throws Exception {
        AlipayTradeWapPayResponse r = new AlipayTradeWapPayResponse();
        r.setCode("10000");
        r.setBody("https://openapi.alipay.com/gateway.do?sign=xx");
        when(client.pageExecute(any(AlipayTradeWapPayRequest.class), eq("GET"))).thenReturn(r);

        Map<String, String> params = channel.prepay(new PrepayCommand(PayType.ALIPAY_WAP, "P1", "NO1",
                new BigDecimal("88.8"), "优选商城 订单 NO1", LocalDateTime.of(2026, 10, 31, 12, 30, 0), null, "1.2.3.4",
                "https://shop.example.com/pay/result?orderId=1"));

        ArgumentCaptor<AlipayTradeWapPayRequest> req = ArgumentCaptor.forClass(AlipayTradeWapPayRequest.class);
        verify(client).pageExecute(req.capture(), eq("GET"));
        AlipayTradeWapPayModel m = (AlipayTradeWapPayModel) req.getValue().getBizModel();
        assertThat(m.getOutTradeNo()).isEqualTo("P1");
        assertThat(m.getTotalAmount()).isEqualTo("88.80");
        assertThat(m.getProductCode()).isEqualTo("QUICK_WAP_WAY");
        assertThat(m.getTimeExpire()).isEqualTo("2026-10-31 12:30:00");
        assertThat(req.getValue().getNotifyUrl()).endsWith("/notify/alipay");
        assertThat(req.getValue().getReturnUrl()).contains("orderId=1");
        assertThat(params).containsEntry("payUrl", "https://openapi.alipay.com/gateway.do?sign=xx");
    }

    private static Map<String, String> notify(String appId, String status, String amount) {
        Map<String, String> p = new HashMap<>();
        p.put("app_id", appId);
        p.put("out_trade_no", "P1");
        p.put("trade_no", "2026103122001");
        p.put("trade_status", status);
        p.put("total_amount", amount);
        p.put("sign", "s");
        p.put("sign_type", "RSA2");
        return p;
    }

    @Test
    void 通知验签通过_金额转为分() {
        when(verifier.verify(anyMap(), eq("alipay-public-key"))).thenReturn(true);
        ChannelTrade t = channel.parseNotify(notify("2021000000", "TRADE_SUCCESS", "88.80"));
        assertThat(t.paid()).isTrue();
        assertThat(t.amountFen()).isEqualTo(8880);
        assertThat(t.channelNo()).isEqualTo("2026103122001");
    }

    @Test
    void 通知验签失败_拒绝() {
        when(verifier.verify(anyMap(), any())).thenReturn(false);
        assertThatThrownBy(() -> channel.parseNotify(notify("2021000000", "TRADE_SUCCESS", "88.80")))
                .isInstanceOf(PayNotifyException.class);
    }

    @Test
    void 通知app_id不符_拒绝() {
        when(verifier.verify(anyMap(), any())).thenReturn(true);
        assertThatThrownBy(() -> channel.parseNotify(notify("other", "TRADE_SUCCESS", "88.80")))
                .isInstanceOf(PayNotifyException.class);
    }

    @Test
    void 退款引起的关闭通知不视为支付() {
        when(verifier.verify(anyMap(), any())).thenReturn(true);
        assertThat(channel.parseNotify(notify("2021000000", "TRADE_CLOSED", "88.80")).paid()).isFalse();
    }

    @Test
    void 查单_交易不存在() throws Exception {
        AlipayTradeQueryResponse r = new AlipayTradeQueryResponse();
        r.setCode("40004");
        r.setSubCode("ACQ.TRADE_NOT_EXIST");
        when(client.execute(any(AlipayTradeQueryRequest.class))).thenReturn(r);
        Payment p = new Payment();
        p.setPayNo("P1");
        assertThat(channel.query(p).state()).isEqualTo(ChannelTrade.State.NOT_EXIST);
    }

    @Test
    void 退款_资金已变动即成功() throws Exception {
        AlipayTradeRefundResponse r = new AlipayTradeRefundResponse();
        r.setCode("10000");
        r.setFundChange("Y");
        when(client.execute(any(AlipayTradeRefundRequest.class))).thenReturn(r);

        var result = channel.refund(new RefundCommand(1L, 2L, "NO1", "P1", "A1", new BigDecimal("10.00")));
        assertThat(result.status()).isEqualTo(RefundStatus.SUCCESS);
    }

    @Test
    void 退款_资金未变动时以退款查询为准() throws Exception {
        AlipayTradeRefundResponse r = new AlipayTradeRefundResponse();
        r.setCode("10000");
        r.setFundChange("N");
        when(client.execute(any(AlipayTradeRefundRequest.class))).thenReturn(r);
        AlipayTradeFastpayRefundQueryResponse q = new AlipayTradeFastpayRefundQueryResponse();
        q.setCode("10000");
        q.setOutRequestNo("A1");
        q.setRefundStatus("REFUND_SUCCESS");
        when(client.execute(any(AlipayTradeFastpayRefundQueryRequest.class))).thenReturn(q);

        var result = channel.refund(new RefundCommand(1L, 2L, "NO1", "P1", "A1", new BigDecimal("10.00")));
        assertThat(result.status()).isEqualTo(RefundStatus.SUCCESS);
    }

    @Test
    void 未开启时不可用() {
        when(settings.alipay()).thenReturn(new PaySettings.Alipay(false, "", "", "", "g"));
        assertThat(channel.availability(PayType.ALIPAY_WAP).on()).isFalse();
    }
}
