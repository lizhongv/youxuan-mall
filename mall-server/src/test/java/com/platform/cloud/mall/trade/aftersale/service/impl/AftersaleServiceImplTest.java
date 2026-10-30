package com.platform.cloud.mall.trade.aftersale.service.impl;

import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.trade.MpTestSupport;
import com.platform.cloud.mall.trade.aftersale.dto.AftersaleApplyRequest;
import com.platform.cloud.mall.trade.aftersale.dto.AftersaleReturnRequest;
import com.platform.cloud.mall.trade.aftersale.entity.Aftersale;
import com.platform.cloud.mall.trade.aftersale.mapper.AftersaleMapper;
import com.platform.cloud.mall.trade.common.TradeSettings;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.entity.OrderItem;
import com.platform.cloud.mall.trade.order.enums.OrderStatus;
import com.platform.cloud.mall.trade.order.mapper.OrderItemMapper;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.order.service.OrderStateMachine;
import com.platform.cloud.mall.trade.pay.service.PayService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AftersaleServiceImplTest {

    private static final Long MEMBER = 1001L;
    private static final Long ORDER_ID = 5001L;
    private static final Long ITEM_ID = 6001L;
    private static final Long AS_ID = 7001L;

    @Mock
    AftersaleMapper aftersaleMapper;
    @Mock
    OrderMapper orderMapper;
    @Mock
    OrderItemMapper orderItemMapper;
    @Mock
    OrderStateMachine orderStateMachine;
    @Mock
    PayService payService;
    @Mock
    GoodsApi goodsApi;
    @Mock
    MemberApi memberApi;
    @Mock
    TradeSettings settings;
    @InjectMocks
    AftersaleServiceImpl service;

    @BeforeAll
    static void initMp() {
        MpTestSupport.init(Order.class, OrderItem.class, Aftersale.class);
    }

    @BeforeEach
    void setUp() {
        when(settings.aftersaleDays()).thenReturn(7);
        when(orderItemMapper.selectOne(any())).thenReturn(item());
        when(orderItemMapper.selectById(ITEM_ID)).thenReturn(item());
        when(orderItemMapper.update(isNull(), any())).thenReturn(1);
        when(aftersaleMapper.update(isNull(), any())).thenReturn(1);
        when(aftersaleMapper.sumRefundedByItem(ITEM_ID)).thenReturn(BigDecimal.ZERO);
    }

    private static OrderItem item() {
        OrderItem i = new OrderItem();
        i.setId(ITEM_ID);
        i.setOrderId(ORDER_ID);
        i.setMemberId(MEMBER);
        i.setSkuId(11L);
        i.setQuantity(2);
        i.setPayAmount(new BigDecimal("50.00"));
        i.setAftersaleStatus(OrderItem.AS_NONE);
        return i;
    }

    private void orderIs(OrderStatus status, LocalDateTime receiveTime) {
        Order o = new Order();
        o.setId(ORDER_ID);
        o.setOrderNo("20261031120000123456");
        o.setMemberId(MEMBER);
        o.setStatus(status.name());
        o.setPayType("BALANCE");
        o.setReceiveTime(receiveTime);
        when(orderMapper.selectOne(any())).thenReturn(o);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(o);
    }

    private static AftersaleApplyRequest apply(String type, String amount) {
        AftersaleApplyRequest req = new AftersaleApplyRequest();
        req.setOrderItemId(ITEM_ID);
        req.setType(type);
        req.setReason("质量问题");
        req.setAmount(new BigDecimal(amount));
        return req;
    }

    private Aftersale aftersale(String type, String status, String amount) {
        Aftersale a = new Aftersale();
        a.setId(AS_ID);
        a.setAftersaleNo("A20261031120000000001");
        a.setOrderId(ORDER_ID);
        a.setOrderItemId(ITEM_ID);
        a.setMemberId(MEMBER);
        a.setType(type);
        a.setStatus(status);
        a.setAmount(new BigDecimal(amount));
        when(aftersaleMapper.selectById(AS_ID)).thenReturn(a);
        when(aftersaleMapper.selectOne(any())).thenReturn(a);
        return a;
    }

    // ------------------------------------------------------------------ 申请：金额上限

    @Test
    void 申请金额不能超过实付减已退() {
        orderIs(OrderStatus.DELIVERED, null);
        when(aftersaleMapper.sumRefundedByItem(ITEM_ID)).thenReturn(new BigDecimal("20.00"));

        assertThatThrownBy(() -> service.apply(MEMBER, apply("REFUND", "30.01")))
                .hasMessageContaining("不能超过 30.00");
        verify(aftersaleMapper, never()).insert(any(Aftersale.class));
    }

    @Test
    void 申请金额等于剩余额度可以提交() {
        orderIs(OrderStatus.DELIVERED, null);
        when(aftersaleMapper.sumRefundedByItem(ITEM_ID)).thenReturn(new BigDecimal("20.00"));

        service.apply(MEMBER, apply("RETURN_REFUND", "30.00"));

        ArgumentCaptor<Aftersale> a = ArgumentCaptor.forClass(Aftersale.class);
        verify(aftersaleMapper).insert(a.capture());
        assertThat(a.getValue().getStatus()).isEqualTo("APPLYING");
        assertThat(a.getValue().getAmount()).isEqualByComparingTo("30.00");
        assertThat(a.getValue().getAftersaleNo()).startsWith("A");
        assertThat(a.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    void 已全部退款不能再申请() {
        orderIs(OrderStatus.DELIVERED, null);
        when(aftersaleMapper.sumRefundedByItem(ITEM_ID)).thenReturn(new BigDecimal("50.00"));
        assertThatThrownBy(() -> service.apply(MEMBER, apply("REFUND", "1"))).hasMessageContaining("已全部退款");
    }

    @Test
    void 同一明细已有进行中售后时拒绝() {
        orderIs(OrderStatus.DELIVERED, null);
        when(orderItemMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.apply(MEMBER, apply("REFUND", "10")))
                .hasMessageContaining("进行中的售后");
        verify(aftersaleMapper, never()).insert(any(Aftersale.class));
    }

    @Test
    void 未发货只能仅退款且须全额() {
        orderIs(OrderStatus.PENDING_DELIVERY, null);
        assertThatThrownBy(() -> service.apply(MEMBER, apply("RETURN_REFUND", "50")))
                .hasMessageContaining("仅支持仅退款");
        assertThatThrownBy(() -> service.apply(MEMBER, apply("REFUND", "10")))
                .hasMessageContaining("全额退款");
    }

    @Test
    void 超过售后期不能申请() {
        orderIs(OrderStatus.COMPLETED, LocalDateTime.now().minusDays(8));
        assertThatThrownBy(() -> service.apply(MEMBER, apply("REFUND", "10"))).hasMessageContaining("售后期");
    }

    @Test
    void 售后期内可申请() {
        orderIs(OrderStatus.COMPLETED, LocalDateTime.now().minusDays(6));
        service.apply(MEMBER, apply("REFUND", "10"));
        verify(aftersaleMapper).insert(any(Aftersale.class));
    }

    @Test
    void 待付款或已取消订单不能申请() {
        orderIs(OrderStatus.PENDING_PAY, null);
        assertThatThrownBy(() -> service.apply(MEMBER, apply("REFUND", "10"))).hasMessageContaining("不能申请售后");
        orderIs(OrderStatus.CANCELLED, null);
        assertThatThrownBy(() -> service.apply(MEMBER, apply("REFUND", "10"))).hasMessageContaining("不能申请售后");
    }

    @Test
    void 他人订单明细视为不存在() {
        when(orderItemMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.apply(MEMBER, apply("REFUND", "10"))).hasMessage("订单商品不存在");
    }

    // ------------------------------------------------------------------ 审核与退款

    @Test
    void 同意仅退款_直接退款_未发货回补库存_全部退完订单取消() {
        orderIs(OrderStatus.PENDING_DELIVERY, null);
        aftersale("REFUND", "APPLYING", "50.00");
        when(orderItemMapper.selectCount(any())).thenReturn(0L);

        service.approve(9L, AS_ID, "同意");

        verify(payService).refund(any(Order.class), eq(new BigDecimal("50.00")), eq("A20261031120000000001"));
        verify(goodsApi).restoreStock(11L, 2);
        verify(orderStateMachine).transit(any(Order.class), eq(OrderStatus.CANCELLED), eq("admin:9"), anyString(),
                any());
        verify(payService).markRefunded(ORDER_ID);
    }

    @Test
    void 未发货部分明细退款不取消订单() {
        orderIs(OrderStatus.PENDING_DELIVERY, null);
        aftersale("REFUND", "APPLYING", "50.00");
        when(orderItemMapper.selectCount(any())).thenReturn(1L);

        service.approve(9L, AS_ID, null);

        verify(goodsApi).restoreStock(11L, 2);
        verify(orderStateMachine, never()).transit(any(), any(), anyString(), anyString(), any());
    }

    @Test
    void 已发货退款不回补库存() {
        orderIs(OrderStatus.DELIVERED, null);
        aftersale("REFUND", "APPLYING", "20.00");

        service.approve(9L, AS_ID, null);

        verify(payService).refund(any(Order.class), eq(new BigDecimal("20.00")), anyString());
        verify(goodsApi, never()).restoreStock(anyLong(), anyInt());
    }

    @Test
    void 同意退货退款进入待退货不退款() {
        orderIs(OrderStatus.DELIVERED, null);
        Aftersale a = aftersale("RETURN_REFUND", "APPLYING", "20.00");

        service.approve(9L, AS_ID, null);

        assertThat(a.getStatus()).isEqualTo("WAIT_RETURN");
        verify(payService, never()).refund(any(), any(), anyString());
    }

    @Test
    void 重复审核被拒绝() {
        aftersale("REFUND", "REFUNDED", "20.00");
        assertThatThrownBy(() -> service.approve(9L, AS_ID, null)).hasMessageContaining("仅申请中");
        verify(payService, never()).refund(any(), any(), anyString());
    }

    @Test
    void 并发审核条件更新失败时不退款() {
        orderIs(OrderStatus.DELIVERED, null);
        aftersale("REFUND", "APPLYING", "20.00");
        when(aftersaleMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.approve(9L, AS_ID, null)).hasMessageContaining("状态已变化");
        verify(payService, never()).refund(any(), any(), anyString());
    }

    @Test
    void 退款时额度复核_超出则拒绝() {
        orderIs(OrderStatus.DELIVERED, null);
        aftersale("REFUND", "APPLYING", "40.00");
        when(aftersaleMapper.sumRefundedByItem(ITEM_ID)).thenReturn(new BigDecimal("20.00"));

        assertThatThrownBy(() -> service.approve(9L, AS_ID, null)).hasMessageContaining("超过可退金额");
        verify(payService, never()).refund(any(), any(), anyString());
    }

    @Test
    void 退货流程_填物流后商家确认收货退款() {
        orderIs(OrderStatus.DELIVERED, null);
        Aftersale a = aftersale("RETURN_REFUND", "WAIT_RETURN", "20.00");
        AftersaleReturnRequest r = new AftersaleReturnRequest();
        r.setReturnCompany("中通");
        r.setReturnNo("ZT1");

        service.memberReturn(MEMBER, AS_ID, r);
        assertThat(a.getStatus()).isEqualTo("RETURNING");

        service.receiveReturn(9L, AS_ID);
        assertThat(a.getStatus()).isEqualTo("REFUNDED");
        verify(payService).refund(any(Order.class), eq(new BigDecimal("20.00")), anyString());
    }

    @Test
    void 未填物流不能确认收货() {
        aftersale("RETURN_REFUND", "WAIT_RETURN", "20.00");
        assertThatThrownBy(() -> service.receiveReturn(9L, AS_ID)).hasMessageContaining("仅退货中");
    }

    @Test
    void 拒绝后释放明细() {
        Aftersale a = aftersale("REFUND", "APPLYING", "20.00");

        service.reject(9L, AS_ID, "不符合");

        assertThat(a.getStatus()).isEqualTo("REJECTED");
        // 售后单条件更新 1 次，明细释放 1 次
        verify(orderItemMapper).update(isNull(), any());
    }

    @Test
    void 退货中不能撤销() {
        aftersale("RETURN_REFUND", "RETURNING", "20.00");
        assertThatThrownBy(() -> service.memberCancel(MEMBER, AS_ID)).hasMessageContaining("不能撤销");
    }

    @Test
    void 待退货可以撤销() {
        Aftersale a = aftersale("RETURN_REFUND", "WAIT_RETURN", "20.00");
        service.memberCancel(MEMBER, AS_ID);
        assertThat(a.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void 撤销他人售后视为不存在() {
        when(aftersaleMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.memberCancel(MEMBER, AS_ID)).hasMessage("售后单不存在");
    }

    @Test
    void 空分页不查关联数据() {
        when(aftersaleMapper.selectPage(any(), any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.memberPage(MEMBER, new com.platform.cloud.mall.trade.aftersale.dto.AftersalePageQuery())
                .getRecords()).isEmpty();
        verify(orderMapper, never()).selectBatchIds(any());
    }
}
