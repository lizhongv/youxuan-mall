package com.platform.cloud.mall.trade.pay.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.trade.MpTestSupport;
import com.platform.cloud.mall.trade.aftersale.entity.Aftersale;
import com.platform.cloud.mall.trade.aftersale.mapper.AftersaleMapper;
import com.platform.cloud.mall.trade.aftersale.service.AftersaleService;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.order.service.OrderStateMachine;
import com.platform.cloud.mall.trade.pay.channel.AsyncPayChannel;
import com.platform.cloud.mall.trade.pay.channel.ChannelTrade;
import com.platform.cloud.mall.trade.pay.channel.PayChannel.RefundCommand;
import com.platform.cloud.mall.trade.pay.channel.PayChannel.RefundResult;
import com.platform.cloud.mall.trade.pay.entity.Payment;
import com.platform.cloud.mall.trade.pay.mapper.PaymentMapper;
import com.platform.cloud.mall.trade.pay.service.PayService;
import com.platform.cloud.mall.trade.pay.service.PaySettlementService.SettleOutcome;
import com.platform.cloud.mall.trade.pay.support.PayNotifyException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付结算：回调 / 查单共用的幂等落账、金额核对、关单后到账自动退款、查单补偿、退款结果分发。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaySettlementServiceImplTest {

    private static final String PAY_NO = "P20261031120000123456";
    private static final Long ORDER_ID = 5001L;

    @Mock
    AsyncPayChannel wechat;
    @Mock
    PaymentMapper paymentMapper;
    @Mock
    OrderMapper orderMapper;
    @Mock
    OrderStateMachine stateMachine;
    @Mock
    PayService payService;
    @Mock
    AftersaleService aftersaleService;
    @Mock
    AftersaleMapper aftersaleMapper;

    PaySettlementServiceImpl service;
    Payment payment;
    Order order;

    @BeforeAll
    static void initMp() {
        MpTestSupport.init(Payment.class, Order.class, Aftersale.class);
    }

    @BeforeEach
    void setUp() {
        when(wechat.type()).thenReturn("WECHAT");
        service = new PaySettlementServiceImpl(List.of(wechat), paymentMapper, orderMapper, stateMachine, payService,
                aftersaleService, aftersaleMapper);
        payment = new Payment();
        payment.setId(1L);
        payment.setPayNo(PAY_NO);
        payment.setOrderId(ORDER_ID);
        payment.setMemberId(1001L);
        payment.setChannel("WECHAT");
        payment.setAmount(new BigDecimal("88.80"));
        payment.setStatus(Payment.WAITING);
        order = new Order();
        order.setId(ORDER_ID);
        order.setOrderNo("20261031120000123456");
        order.setStatus("PENDING_PAY");
        order.setPayAmount(new BigDecimal("88.80"));
        when(paymentMapper.selectOne(any())).thenReturn(payment);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(paymentMapper.update(isNull(), any())).thenReturn(1);
        when(wechat.refund(any())).thenReturn(RefundResult.processing());
    }

    private static ChannelTrade paid(long fen) {
        return new ChannelTrade(PAY_NO, ChannelTrade.State.PAID, "4200001", fen);
    }

    @Test
    void 支付成功_金额一致_调用统一落账() {
        assertThat(service.settle(paid(8880))).isEqualTo(SettleOutcome.PAID);
        verify(payService).onPaid(PAY_NO, "4200001");
        verify(wechat, never()).refund(any());
    }

    @Test
    void 金额不符_拒绝落账() {
        assertThatThrownBy(() -> service.settle(paid(1))).isInstanceOf(PayNotifyException.class);
        verify(payService, never()).onPaid(anyString(), anyString());
    }

    @Test
    void 重复通知_已成功的流水直接返回() {
        payment.setStatus(Payment.SUCCESS);
        assertThat(service.settle(paid(8880))).isEqualTo(SettleOutcome.ALREADY);
        verify(payService, never()).onPaid(anyString(), anyString());
    }

    @Test
    void 未支付状态的通知忽略() {
        assertThat(service.settle(new ChannelTrade(PAY_NO, ChannelTrade.State.CLOSED, "", 8880)))
                .isEqualTo(SettleOutcome.IGNORED);
    }

    @Test
    void 未知支付单_拒绝() {
        when(paymentMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.settle(paid(8880))).isInstanceOf(PayNotifyException.class);
    }

    @Test
    void 关单后到账_自动原路退款并标记() {
        order.setStatus("CANCELLED");

        assertThat(service.settle(paid(8880))).isEqualTo(SettleOutcome.REFUNDED_AFTER_CLOSE);

        verify(payService, never()).onPaid(anyString(), anyString());
        ArgumentCaptor<RefundCommand> cmd = ArgumentCaptor.forClass(RefundCommand.class);
        verify(wechat).refund(cmd.capture());
        assertThat(cmd.getValue().refundNo()).isEqualTo("R" + PAY_NO);
        assertThat(cmd.getValue().amount()).isEqualByComparingTo("88.80");
        verify(stateMachine).log(eq(ORDER_ID), eq("CANCELLED"), eq("CANCELLED"), eq("system"), anyString());
    }

    @Test
    void 关单后到账_并发已处理时不重复退款() {
        order.setStatus("CANCELLED");
        when(paymentMapper.update(isNull(), any())).thenReturn(0);

        assertThat(service.settle(paid(8880))).isEqualTo(SettleOutcome.ALREADY);
        verify(wechat, never()).refund(any());
    }

    @Test
    void 关单后到账_退款失败时还原状态等待重试() {
        order.setStatus("CANCELLED");
        when(wechat.refund(any())).thenThrow(new BusinessException("网络错误"));

        assertThatThrownBy(() -> service.settle(paid(8880))).hasMessageContaining("等待重试");
        // 一次占位 + 一次还原
        verify(paymentMapper, times(2)).update(isNull(), any());
    }

    @Test
    void 订单已被其他支付单付清_重复支付自动退款() {
        order.setStatus("PENDING_DELIVERY");
        order.setPayNo("P-OTHER");
        order.setPayTime(LocalDateTime.now());
        assertThat(service.settle(paid(8880))).isEqualTo(SettleOutcome.REFUNDED_AFTER_CLOSE);
    }

    @Test
    void 订单改价后旧预支付单到账_自动退款() {
        order.setPayAmount(new BigDecimal("80.00"));
        assertThat(service.settle(paid(8880))).isEqualTo(SettleOutcome.REFUNDED_AFTER_CLOSE);
        verify(payService, never()).onPaid(anyString(), anyString());
    }

    @Test
    void 落账与关单并发_订单已关闭则转为自动退款() {
        doThrow(new BusinessException("订单状态已变化")).when(payService).onPaid(anyString(), anyString());
        Order closed = new Order();
        closed.setId(ORDER_ID);
        closed.setStatus("CANCELLED");
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order, closed);

        assertThat(service.settle(paid(8880))).isEqualTo(SettleOutcome.REFUNDED_AFTER_CLOSE);
        verify(wechat).refund(any());
    }

    @Test
    void 已关闭的旧支付单在订单仍待付款时到账_恢复后照常落账() {
        payment.setStatus(Payment.CLOSED);
        assertThat(service.settle(paid(8880))).isEqualTo(SettleOutcome.PAID);
        verify(payService).onPaid(PAY_NO, "4200001");
    }

    @Test
    void 查单补偿_渠道已支付则落账() {
        when(wechat.query(payment)).thenReturn(paid(8880));
        service.sync(payment);
        verify(payService).onPaid(PAY_NO, "4200001");
    }

    @Test
    void 查单补偿_订单已关闭且渠道未支付_关闭渠道订单() {
        order.setStatus("CANCELLED");
        when(wechat.query(payment)).thenReturn(new ChannelTrade(PAY_NO, ChannelTrade.State.WAITING, "", 8880));

        service.sync(payment);

        verify(wechat).close(payment);
        verify(paymentMapper).update(isNull(), any());
    }

    @Test
    void 查单补偿_订单仍可支付且未付_不动() {
        when(wechat.query(payment)).thenReturn(new ChannelTrade(PAY_NO, ChannelTrade.State.WAITING, "", 8880));
        service.sync(payment);
        verify(wechat, never()).close(any());
        verify(paymentMapper, never()).update(isNull(), any());
    }

    @Test
    void 查单补偿_关单后到账_自动退款() {
        order.setStatus("CANCELLED");
        when(wechat.query(payment)).thenReturn(paid(8880));
        service.sync(payment);
        verify(wechat).refund(any());
    }

    @Test
    void 补偿任务批量查单_单条失败不影响其他() {
        Payment other = new Payment();
        other.setPayNo("P2");
        other.setStatus(Payment.WAITING);
        other.setChannel("WECHAT");
        Page<Payment> page = new Page<>();
        page.setRecords(List.of(other, payment));
        when(paymentMapper.selectPage(any(), any())).thenReturn(page);
        when(wechat.query(other)).thenThrow(new BusinessException("超时"));
        when(wechat.query(payment)).thenReturn(paid(8880));

        assertThat(service.compensateWaiting(50)).isEqualTo(1);
        verify(payService).onPaid(PAY_NO, "4200001");
    }

    @Test
    void 已成功的流水不再查单() {
        payment.setStatus(Payment.SUCCESS);
        service.sync(payment);
        verify(wechat, never()).query(any());
    }

    @Test
    void 退款结果分发_售后单号交给售后领域() {
        service.onRefundResult("A1", RefundResult.success());
        verify(aftersaleService).onChannelRefundSuccess("A1");
        service.onRefundResult("A2", RefundResult.failed("异常"));
        verify(aftersaleService).onChannelRefundFailed("A2", "异常");
        service.onRefundResult("A3", RefundResult.processing());
        verify(aftersaleService, never()).onChannelRefundSuccess("A3");
    }

    @Test
    void 退款结果分发_关单后到账退款不进售后() {
        service.onRefundResult("R" + PAY_NO, RefundResult.failed("异常"));
        verify(aftersaleService, never()).onChannelRefundFailed(anyString(), anyString());
        verify(paymentMapper).update(isNull(), any());
    }

    @Test
    void 退款补偿_查询退款中的售后单() {
        Aftersale a = new Aftersale();
        a.setAftersaleNo("A9");
        a.setOrderId(ORDER_ID);
        Page<Aftersale> page = new Page<>();
        page.setRecords(List.of(a));
        when(aftersaleMapper.selectPage(any(), any())).thenReturn(page);
        order.setPayType("WECHAT");
        order.setPayNo(PAY_NO);
        when(wechat.queryRefund(PAY_NO, "A9")).thenReturn(RefundResult.success());

        assertThat(service.syncRefunding(50)).isEqualTo(1);
        verify(aftersaleService).onChannelRefundSuccess("A9");
    }
}
