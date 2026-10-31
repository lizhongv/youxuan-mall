package com.platform.cloud.mall.trade.pay.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.member.api.MemberApi.MemberBrief;
import com.platform.cloud.mall.trade.MpTestSupport;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.pay.PayProperties;
import com.platform.cloud.mall.trade.pay.PayType;
import com.platform.cloud.mall.trade.pay.channel.AsyncPayChannel;
import com.platform.cloud.mall.trade.pay.channel.AsyncPayChannel.Availability;
import com.platform.cloud.mall.trade.pay.channel.AsyncPayChannel.PrepayCommand;
import com.platform.cloud.mall.trade.pay.channel.ChannelTrade;
import com.platform.cloud.mall.trade.pay.config.PaySettings;
import com.platform.cloud.mall.trade.pay.entity.Payment;
import com.platform.cloud.mall.trade.pay.mapper.PaymentMapper;
import com.platform.cloud.mall.trade.pay.service.PayService;
import com.platform.cloud.mall.trade.pay.service.PaySettlementService;
import com.platform.cloud.mall.trade.pay.service.PaySettlementService.SettleOutcome;
import com.platform.cloud.mall.trade.pay.vo.PayChannelVO;
import com.platform.cloud.mall.trade.pay.vo.PayLaunchVO;
import com.platform.cloud.mall.trade.pay.vo.PayStatusVO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 收银台：渠道可用性判断、同步 / 异步发起、预支付单复用与关闭、支付结果轮询查单。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CashierServiceImplTest {

    private static final Long MEMBER = 1001L;
    private static final Long ORDER_ID = 5001L;

    @Mock
    AsyncPayChannel wechat;
    @Mock
    AsyncPayChannel alipay;
    @Mock
    OrderMapper orderMapper;
    @Mock
    PaymentMapper paymentMapper;
    @Mock
    PayService payService;
    @Mock
    PaySettlementService settlement;
    @Mock
    MemberApi memberApi;
    @Mock
    PaySettings settings;
    @Mock
    StringRedisTemplate redis;
    @Mock
    ValueOperations<String, String> valueOps;

    PayProperties props = new PayProperties();
    CashierServiceImpl service;
    Order order;

    @BeforeAll
    static void initMp() {
        MpTestSupport.init(Order.class, Payment.class);
    }

    @BeforeEach
    void setUp() {
        when(wechat.type()).thenReturn("WECHAT");
        when(alipay.type()).thenReturn("ALIPAY");
        service = new CashierServiceImpl(List.of(wechat, alipay), orderMapper, paymentMapper, payService, settlement,
                memberApi, settings, props, new ObjectMapper(), redis);
        order = new Order();
        order.setId(ORDER_ID);
        order.setOrderNo("20261031120000123456");
        order.setMemberId(MEMBER);
        order.setStatus("PENDING_PAY");
        order.setPayAmount(new BigDecimal("88.80"));
        order.setPayExpireAt(LocalDateTime.now().plusMinutes(20));
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(wechat.availability(any())).thenReturn(Availability.available());
        when(alipay.availability(any())).thenReturn(Availability.available());
        when(memberApi.getMember(MEMBER)).thenReturn(new MemberBrief(MEMBER, "13800000000", "n", "", 1L, "普通",
                BigDecimal.ONE, new BigDecimal("100.00"), 0, 1));
        when(memberApi.getOpenid(MEMBER, MemberApi.PLATFORM_WECHAT_MP)).thenReturn("openid-1");
        when(paymentMapper.selectList(any())).thenReturn(List.of());
        when(paymentMapper.update(isNull(), any())).thenReturn(1);
        when(settings.mallName()).thenReturn("优选商城");
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }

    private static PayChannelVO find(List<PayChannelVO> list, PayType t) {
        return list.stream().filter(v -> v.getType().equals(t.name())).findFirst().orElse(null);
    }

    @Test
    void 渠道列表_微信内_公众号支付可用_支付宝提示浏览器打开() {
        List<PayChannelVO> list = service.channels(MEMBER, ORDER_ID, true);

        assertThat(find(list, PayType.BALANCE).getEnabled()).isTrue();
        assertThat(find(list, PayType.WECHAT_JSAPI).getEnabled()).isTrue();
        assertThat(find(list, PayType.WECHAT_H5).getEnabled()).isFalse();
        assertThat(find(list, PayType.ALIPAY_WAP).getEnabled()).isFalse();
        assertThat(find(list, PayType.ALIPAY_WAP).getReason()).contains("浏览器");
        assertThat(find(list, PayType.MOCK).getEnabled()).isTrue();
    }

    @Test
    void 渠道列表_浏览器内_H5与支付宝可用_公众号支付不可用() {
        List<PayChannelVO> list = service.channels(MEMBER, ORDER_ID, false);
        assertThat(find(list, PayType.WECHAT_JSAPI).getEnabled()).isFalse();
        assertThat(find(list, PayType.WECHAT_H5).getEnabled()).isTrue();
        assertThat(find(list, PayType.ALIPAY_WAP).getEnabled()).isTrue();
    }

    @Test
    void 渠道列表_余额不足_未绑定openid_后台关闭的不展示_模拟关闭不展示() {
        order.setPayAmount(new BigDecimal("188.00"));
        when(memberApi.getOpenid(MEMBER, MemberApi.PLATFORM_WECHAT_MP)).thenReturn(null);
        when(alipay.availability(any())).thenReturn(Availability.off());
        when(wechat.availability(PayType.WECHAT_H5)).thenReturn(Availability.notReady("未配置"));
        props.setMockEnabled(false);

        List<PayChannelVO> list = service.channels(MEMBER, ORDER_ID, true);

        assertThat(find(list, PayType.BALANCE).getEnabled()).isFalse();
        assertThat(find(list, PayType.BALANCE).getReason()).contains("余额不足");
        assertThat(find(list, PayType.WECHAT_JSAPI).getReason()).contains("微信授权登录");
        assertThat(find(list, PayType.WECHAT_H5).getReason()).isEqualTo("未配置");
        assertThat(find(list, PayType.ALIPAY_WAP)).isNull();
        assertThat(find(list, PayType.MOCK)).isNull();
    }

    @Test
    void 同步渠道_交给PayService落账() {
        PayLaunchVO vo = service.pay(MEMBER, ORDER_ID, "BALANCE", "135790", "1.2.3.4");
        assertThat(vo.getPaid()).isTrue();
        verify(payService).pay(MEMBER, ORDER_ID, "BALANCE");
    }

    @Test
    void 余额支付先校验支付密码再扣款() {
        service.pay(MEMBER, ORDER_ID, "BALANCE", "135790", "1.2.3.4");

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(memberApi, payService);
        order.verify(memberApi).verifyPayPassword(MEMBER, "135790");
        order.verify(payService).pay(MEMBER, ORDER_ID, "BALANCE");
    }

    @Test
    void 余额支付密码校验失败时不扣款也不处理旧支付单() {
        org.mockito.Mockito.doThrow(new com.platform.cloud.bootstrap.common.exception.BusinessException(
                        com.platform.cloud.bootstrap.common.web.ResultCode.PAY_PASSWORD_WRONG, "支付密码错误，还可尝试 4 次"))
                .when(memberApi).verifyPayPassword(MEMBER, "000000");

        assertThatThrownBy(() -> service.pay(MEMBER, ORDER_ID, "BALANCE", "000000", ""))
                .hasFieldOrPropertyWithValue("code", 60011);
        verify(payService, never()).pay(any(), any(), anyString());
        verify(paymentMapper, never()).selectList(any());
    }

    @Test
    void 非余额支付不校验支付密码() {
        props.setMockEnabled(true);
        service.pay(MEMBER, ORDER_ID, "MOCK", null, "");
        verify(memberApi, never()).verifyPayPassword(any(), any());
    }

    @Test
    void 模拟支付关闭时不可用() {
        props.setMockEnabled(false);
        assertThatThrownBy(() -> service.pay(MEMBER, ORDER_ID, "MOCK", null, "")).hasMessageContaining("不支持");
    }

    @Test
    void 公众号支付_写流水_下单_保存调起参数() {
        when(wechat.prepay(any())).thenReturn(Map.of("package", "prepay_id=1", "paySign", "s"));

        PayLaunchVO vo = service.pay(MEMBER, ORDER_ID, "WECHAT_JSAPI", null, "1.2.3.4");

        assertThat(vo.getPaid()).isFalse();
        assertThat(vo.getPayParams()).containsEntry("package", "prepay_id=1");
        ArgumentCaptor<Payment> p = ArgumentCaptor.forClass(Payment.class);
        verify(paymentMapper).insert(p.capture());
        assertThat(p.getValue().getChannel()).isEqualTo("WECHAT");
        assertThat(p.getValue().getTradeType()).isEqualTo("WECHAT_JSAPI");
        assertThat(p.getValue().getStatus()).isEqualTo(Payment.WAITING);
        ArgumentCaptor<PrepayCommand> cmd = ArgumentCaptor.forClass(PrepayCommand.class);
        verify(wechat).prepay(cmd.capture());
        assertThat(cmd.getValue().openid()).isEqualTo("openid-1");
        assertThat(cmd.getValue().amount()).isEqualByComparingTo("88.80");
        assertThat(cmd.getValue().expireAt()).isEqualTo(order.getPayExpireAt());
        assertThat(cmd.getValue().subject()).contains("优选商城").contains(order.getOrderNo());
        verify(paymentMapper).update(isNull(), any());
    }

    @Test
    void 公众号支付_未绑定openid_拒绝() {
        when(memberApi.getOpenid(MEMBER, MemberApi.PLATFORM_WECHAT_MP)).thenReturn(null);
        assertThatThrownBy(() -> service.pay(MEMBER, ORDER_ID, "WECHAT_JSAPI", null, "")).hasMessageContaining("微信授权");
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    @Test
    void 渠道不可用时拒绝() {
        when(alipay.availability(any())).thenReturn(Availability.notReady("支付宝暂未配置完成"));
        assertThatThrownBy(() -> service.pay(MEMBER, ORDER_ID, "ALIPAY_WAP", null, "")).hasMessageContaining("未配置");
    }

    @Test
    void 下单失败_关闭流水并提示() {
        when(alipay.prepay(any())).thenThrow(new BusinessException("支付宝下单失败：参数错误"));
        assertThatThrownBy(() -> service.pay(MEMBER, ORDER_ID, "ALIPAY_WAP", null, "")).hasMessageContaining("参数错误");
        verify(paymentMapper).update(isNull(), any());
    }

    private Payment waiting(String tradeType, String channel, LocalDateTime createdAt) {
        Payment p = new Payment();
        p.setId(9L);
        p.setPayNo("P-OLD");
        p.setOrderId(ORDER_ID);
        p.setChannel(channel);
        p.setTradeType(tradeType);
        p.setAmount(new BigDecimal("88.80"));
        p.setStatus(Payment.WAITING);
        p.setPayParams("{\"h5Url\":\"https://wx/h5\"}");
        p.setCreatedAt(createdAt);
        return p;
    }

    @Test
    void 重复发起_未过期的同方式预支付单直接复用() {
        when(paymentMapper.selectList(any())).thenReturn(List.of(waiting("WECHAT_H5", "WECHAT",
                LocalDateTime.now().minusMinutes(1))));

        PayLaunchVO vo = service.pay(MEMBER, ORDER_ID, "WECHAT_H5", null, "1.2.3.4");

        assertThat(vo.getPayNo()).isEqualTo("P-OLD");
        assertThat(vo.getPayParams()).containsEntry("h5Url", "https://wx/h5");
        verify(wechat, never()).prepay(any());
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    @Test
    void 重复发起_换方式时旧单先查单再关单() {
        Payment old = waiting("WECHAT_H5", "WECHAT", LocalDateTime.now().minusMinutes(1));
        when(paymentMapper.selectList(any())).thenReturn(List.of(old));
        when(wechat.query(old)).thenReturn(new ChannelTrade("P-OLD", ChannelTrade.State.WAITING, "", 8880));
        when(alipay.prepay(any())).thenReturn(Map.of("payUrl", "u"));

        service.pay(MEMBER, ORDER_ID, "ALIPAY_WAP", null, "");

        verify(wechat).close(old);
        verify(alipay).prepay(any());
    }

    @Test
    void 重复发起_旧单其实已付款_落账并提示已支付() {
        Payment old = waiting("WECHAT_H5", "WECHAT", LocalDateTime.now().minusMinutes(10));
        when(paymentMapper.selectList(any())).thenReturn(List.of(old));
        ChannelTrade paid = new ChannelTrade("P-OLD", ChannelTrade.State.PAID, "42", 8880);
        when(wechat.query(old)).thenReturn(paid);
        when(settlement.settle(paid)).thenReturn(SettleOutcome.PAID);

        assertThatThrownBy(() -> service.pay(MEMBER, ORDER_ID, "BALANCE", "135790", "")).hasMessageContaining("已支付");
        verify(payService, never()).pay(any(), any(), anyString());
    }

    @Test
    void 重复发起_旧单关单失败时不再发起新支付() {
        Payment old = waiting("WECHAT_H5", "WECHAT", LocalDateTime.now().minusMinutes(10));
        when(paymentMapper.selectList(any())).thenReturn(List.of(old));
        when(wechat.query(old)).thenReturn(new ChannelTrade("P-OLD", ChannelTrade.State.WAITING, "", 8880));
        org.mockito.Mockito.doThrow(new BusinessException("微信关单失败：ORDERPAID")).when(wechat).close(old);

        assertThatThrownBy(() -> service.pay(MEMBER, ORDER_ID, "WECHAT_H5", null, "")).hasMessageContaining("确认中");
        verify(wechat, never()).prepay(any());
    }

    @Test
    void 已支付订单不能发起() {
        order.setStatus("PENDING_DELIVERY");
        assertThatThrownBy(() -> service.pay(MEMBER, ORDER_ID, "WECHAT_H5", null, "")).hasMessageContaining("无需支付");
    }

    @Test
    void 支付状态轮询_本地未支付时主动查单() {
        Payment p = waiting("WECHAT_JSAPI", "WECHAT", LocalDateTime.now());
        when(paymentMapper.selectList(any())).thenReturn(List.of(p));
        Order paidOrder = new Order();
        paidOrder.setId(ORDER_ID);
        paidOrder.setStatus("PENDING_DELIVERY");
        paidOrder.setPayType("WECHAT");
        paidOrder.setPayTime(LocalDateTime.now());
        when(orderMapper.selectOne(any())).thenReturn(order, paidOrder);

        PayStatusVO vo = service.payStatus(MEMBER, ORDER_ID);

        verify(settlement).sync(p);
        assertThat(vo.getPaid()).isTrue();
        assertThat(vo.getStatus()).isEqualTo("PENDING_DELIVERY");
    }

    @Test
    void 支付状态轮询_节流期间不重复查单() {
        when(paymentMapper.selectList(any())).thenReturn(List.of(waiting("WECHAT_JSAPI", "WECHAT",
                LocalDateTime.now())));
        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        PayStatusVO vo = service.payStatus(MEMBER, ORDER_ID);

        verify(settlement, never()).sync(any());
        assertThat(vo.getPaid()).isFalse();
    }
}
