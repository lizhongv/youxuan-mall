package com.platform.cloud.mall.trade.pay.service.impl;

import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.trade.MpTestSupport;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.entity.OrderLog;
import com.platform.cloud.mall.trade.order.mapper.OrderLogMapper;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.order.service.OrderStateMachine;
import com.platform.cloud.mall.trade.pay.PayProperties;
import com.platform.cloud.mall.trade.pay.channel.BalancePayChannel;
import com.platform.cloud.mall.trade.pay.channel.MockPayChannel;
import com.platform.cloud.mall.trade.pay.entity.Payment;
import com.platform.cloud.mall.trade.pay.mapper.PaymentMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PayServiceImplTest {

    private static final Long MEMBER = 1001L;
    private static final Long ORDER_ID = 5001L;

    @Mock
    OrderMapper orderMapper;
    @Mock
    OrderLogMapper orderLogMapper;
    @Mock
    PaymentMapper paymentMapper;
    @Mock
    MemberApi memberApi;

    PayProperties props = new PayProperties();
    PayServiceImpl service;

    @BeforeAll
    static void initMp() {
        MpTestSupport.init(Order.class, OrderLog.class, Payment.class);
    }

    @BeforeEach
    void setUp() {
        service = new PayServiceImpl(List.of(new BalancePayChannel(memberApi), new MockPayChannel(props)),
                orderMapper, paymentMapper, new OrderStateMachine(orderMapper, orderLogMapper));
        when(orderMapper.update(isNull(), any())).thenReturn(1);
    }

    private void orderIs(String status, LocalDateTime expireAt) {
        Order o = new Order();
        o.setId(ORDER_ID);
        o.setOrderNo("20261031120000123456");
        o.setMemberId(MEMBER);
        o.setStatus(status);
        o.setPayAmount(new BigDecimal("88.80"));
        o.setPayExpireAt(expireAt);
        when(orderMapper.selectOne(any())).thenReturn(o);
    }

    @Test
    void 余额支付扣余额并置为待发货() {
        orderIs("PENDING_PAY", LocalDateTime.now().plusMinutes(10));

        assertThat(service.pay(MEMBER, ORDER_ID, "BALANCE")).isTrue();

        verify(memberApi).changeBalance(eq(MEMBER), eq(new BigDecimal("-88.80")), eq("ORDER_PAY"),
                eq("20261031120000123456"), anyString());
        ArgumentCaptor<OrderLog> log = ArgumentCaptor.forClass(OrderLog.class);
        verify(orderLogMapper).insert(log.capture());
        assertThat(log.getValue().getToStatus()).isEqualTo("PENDING_DELIVERY");
        verify(paymentMapper).insert(any(Payment.class));
    }

    @Test
    void 已支付订单不能重复支付() {
        orderIs("PENDING_DELIVERY", LocalDateTime.now().plusMinutes(10));
        assertThatThrownBy(() -> service.pay(MEMBER, ORDER_ID, "BALANCE")).hasMessageContaining("无需支付");
        verify(memberApi, never()).changeBalance(any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void 与关单并发时条件更新失败_整体失败() {
        orderIs("PENDING_PAY", LocalDateTime.now().plusMinutes(10));
        when(orderMapper.update(isNull(), any())).thenReturn(0);
        // 扣款与状态更新在同一事务，这里抛异常即整体回滚
        assertThatThrownBy(() -> service.pay(MEMBER, ORDER_ID, "MOCK")).hasMessageContaining("状态已变化");
    }

    @Test
    void 余额支付时订单已被改价_按金额条件落账失败并提示刷新() {
        orderIs("PENDING_PAY", LocalDateTime.now().plusMinutes(10));
        when(orderMapper.update(isNull(), any())).thenReturn(0);
        Order changed = new Order();
        changed.setId(ORDER_ID);
        changed.setStatus("PENDING_PAY");
        changed.setPayAmount(new BigDecimal("50.00"));
        when(orderMapper.selectById(ORDER_ID)).thenReturn(changed);

        // 抛异常 → 同一事务内的余额扣款一起回滚
        assertThatThrownBy(() -> service.pay(MEMBER, ORDER_ID, "BALANCE")).hasMessage("订单金额已变化，请刷新后重试");
        verify(memberApi).changeBalance(eq(MEMBER), eq(new BigDecimal("-88.80")), anyString(), anyString(),
                anyString());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Order>> w =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(orderMapper).update(isNull(), w.capture());
        assertThat(w.getValue().getSqlSegment()).contains("pay_amount").contains("status");
        assertThat(w.getValue().getParamNameValuePairs().values()).contains(new BigDecimal("88.80"));
    }

    @Test
    void 超时订单不能支付() {
        orderIs("PENDING_PAY", LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> service.pay(MEMBER, ORDER_ID, "MOCK")).hasMessageContaining("超时");
    }

    @Test
    void 关闭模拟支付后不可用_未知渠道不可用() {
        orderIs("PENDING_PAY", LocalDateTime.now().plusMinutes(10));
        props.setMockEnabled(false);
        assertThatThrownBy(() -> service.pay(MEMBER, ORDER_ID, "MOCK")).hasMessageContaining("不支持");
        assertThatThrownBy(() -> service.pay(MEMBER, ORDER_ID, "WECHAT")).hasMessageContaining("不支持");
    }

    @Test
    void 余额支付订单退款退回余额() {
        Order o = new Order();
        o.setId(ORDER_ID);
        o.setOrderNo("NO1");
        o.setMemberId(MEMBER);
        o.setPayType("BALANCE");

        service.refund(o, new BigDecimal("10.00"), "A1");

        verify(memberApi).changeBalance(eq(MEMBER), eq(new BigDecimal("10.00")), eq("ORDER_REFUND"), eq("NO1"),
                anyString());
    }
}
