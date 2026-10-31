package com.platform.cloud.mall.trade.aftersale.service.impl;

import com.platform.cloud.mall.system.api.FileApi;
import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.marketing.api.CouponApi;
import com.platform.cloud.mall.marketing.api.SeckillApi;
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
import com.platform.cloud.mall.trade.pay.channel.PayChannel.RefundResult;
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
    @Mock
    CouponApi couponApi;
    @Mock
    SeckillApi seckillApi;
    @Mock
    FileApi fileApi;
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
        when(payService.refund(any(), any(), anyString())).thenReturn(RefundResult.success());
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

    private Order orderIs(OrderStatus status, LocalDateTime receiveTime) {
        Order o = new Order();
        o.setId(ORDER_ID);
        o.setOrderNo("20261031120000123456");
        o.setMemberId(MEMBER);
        o.setStatus(status.name());
        o.setPayType("BALANCE");
        o.setReceiveTime(receiveTime);
        o.setOrderType("NORMAL");
        o.setPayAmount(new BigDecimal("110.00"));
        o.setFreightAmount(BigDecimal.ZERO);
        when(orderMapper.selectOne(any())).thenReturn(o);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(o);
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(o);
        return o;
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
    void 申请售后先锁订单行_与发货串行() {
        orderIs(OrderStatus.PENDING_DELIVERY, null);

        service.apply(MEMBER, apply("REFUND", "50.00"));

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(orderMapper, orderItemMapper);
        inOrder.verify(orderMapper).selectByIdForUpdate(ORDER_ID);
        inOrder.verify(orderItemMapper).update(isNull(), any());
    }

    @Test
    void 锁到的订单不属于当前会员视为不存在() {
        Order o = orderIs(OrderStatus.DELIVERED, null);
        o.setMemberId(999L);
        assertThatThrownBy(() -> service.apply(MEMBER, apply("REFUND", "10"))).hasMessage("订单不存在");
        verify(aftersaleMapper, never()).insert(any(Aftersale.class));
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
    void 未发货整单退完_额外退运费_退券_归还秒杀名额() {
        Order o = orderIs(OrderStatus.PENDING_DELIVERY, null);
        o.setFreightAmount(new BigDecimal("10.00"));
        o.setMemberCouponId(88L);
        o.setOrderType("SECKILL");
        o.setSeckillId(77L);
        aftersale("REFUND", "APPLYING", "50.00");
        when(orderItemMapper.selectCount(any())).thenReturn(0L);
        when(orderItemMapper.selectList(any())).thenReturn(java.util.List.of(item()));

        service.approve(9L, AS_ID, "同意");

        verify(payService).refund(any(Order.class), eq(new BigDecimal("50.00")), eq("A20261031120000000001"));
        // 运费单独一笔，退款单号与商品退款区分（渠道按单号幂等）
        verify(payService).refund(any(Order.class), eq(new BigDecimal("10.00")), eq("A20261031120000000001F"));
        verify(couponApi).giveBack(88L);
        verify(seckillApi).release(77L, MEMBER, 2);
        verify(payService).markRefunded(ORDER_ID);
        verify(orderStateMachine).log(eq(ORDER_ID), eq("CANCELLED"), eq("CANCELLED"), eq("admin:9"),
                org.mockito.ArgumentMatchers.contains("运费 10.00"));
    }

    @Test
    void 未发货整单退完_无运费无券时不额外退款() {
        orderIs(OrderStatus.PENDING_DELIVERY, null);
        aftersale("REFUND", "APPLYING", "50.00");
        when(orderItemMapper.selectCount(any())).thenReturn(0L);

        service.approve(9L, AS_ID, null);

        verify(payService, org.mockito.Mockito.times(1)).refund(any(Order.class), any(), anyString());
        verify(couponApi, never()).giveBack(anyLong());
        verify(seckillApi, never()).release(anyLong(), anyLong(), anyInt());
    }

    @Test
    void 部分退款不退运费不退券() {
        Order o = orderIs(OrderStatus.PENDING_DELIVERY, null);
        o.setFreightAmount(new BigDecimal("10.00"));
        o.setMemberCouponId(88L);
        aftersale("REFUND", "APPLYING", "50.00");
        when(orderItemMapper.selectCount(any())).thenReturn(1L);

        service.approve(9L, AS_ID, null);

        verify(payService, org.mockito.Mockito.times(1)).refund(any(Order.class), any(), anyString());
        verify(couponApi, never()).giveBack(anyLong());
    }

    @Test
    void 确认收货后退款按比例回收积分与成长值() {
        Order o = orderIs(OrderStatus.COMPLETED, LocalDateTime.now().minusDays(1));
        o.setRewardBase(new BigDecimal("100.00"));
        o.setRewardPoints(100);
        o.setRewardGrowth(51);
        aftersale("REFUND", "APPLYING", "30.00");

        service.approve(9L, AS_ID, null);

        // 100 × 30/100 = 30；51 × 0.3 = 15.3 → 15（向下取整，多次部分退款总和不超过发放量）
        verify(memberApi).reclaimReward(eq(MEMBER), eq(30), eq(15), eq("ORDER_REWARD_REVOKE"),
                eq("A20261031120000000001"), anyString());
        verify(goodsApi, never()).restoreStock(anyLong(), anyInt());
    }

    @Test
    void 历史订单无发放记录时按当前配置折算回收() {
        orderIs(OrderStatus.COMPLETED, LocalDateTime.now().minusDays(1));
        when(settings.pointsPerYuan()).thenReturn(BigDecimal.ONE);
        when(settings.growthPerYuan()).thenReturn(new BigDecimal("0.5"));
        aftersale("REFUND", "APPLYING", "30.99");

        service.approve(9L, AS_ID, null);

        verify(memberApi).reclaimReward(eq(MEMBER), eq(30), eq(15), anyString(), anyString(), anyString());
    }

    @Test
    void 未收货退款不回收奖励() {
        orderIs(OrderStatus.DELIVERED, null);
        aftersale("REFUND", "APPLYING", "20.00");

        service.approve(9L, AS_ID, null);

        verify(memberApi, never()).reclaimReward(anyLong(), anyInt(), anyInt(), anyString(), anyString(), anyString());
    }

    @Test
    void 比例回收向下取整() {
        assertThat(AftersaleServiceImpl.proportional(10, new BigDecimal("33.33"), new BigDecimal("100"))).isEqualTo(3);
        assertThat(AftersaleServiceImpl.proportional(10, new BigDecimal("100"), new BigDecimal("100"))).isEqualTo(10);
        assertThat(AftersaleServiceImpl.proportional(0, new BigDecimal("50"), new BigDecimal("100"))).isZero();
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

    // ------------------------------------------------------------------ 渠道退款（REFUNDING）

    @Test
    void 微信退款处理中_售后保持退款中_不回补库存() {
        orderIs(OrderStatus.PENDING_DELIVERY, null);
        Aftersale a = aftersale("REFUND", "APPLYING", "50.00");
        when(payService.refund(any(), any(), anyString())).thenReturn(RefundResult.processing());

        service.approve(9L, AS_ID, "同意");

        assertThat(a.getStatus()).isEqualTo("REFUNDING");
        verify(goodsApi, never()).restoreStock(anyLong(), anyInt());
        verify(orderItemMapper, never()).update(isNull(), any());
    }

    @Test
    void 退款回调成功_退款中推进为完成并回补库存() {
        orderIs(OrderStatus.PENDING_DELIVERY, null);
        Aftersale a = aftersale("REFUND", "REFUNDING", "50.00");
        when(orderItemMapper.selectCount(any())).thenReturn(1L);

        service.onChannelRefundSuccess("A20261031120000000001");

        assertThat(a.getStatus()).isEqualTo("REFUNDED");
        verify(goodsApi).restoreStock(11L, 2);
    }

    @Test
    void 退款回调重复通知幂等() {
        aftersale("REFUND", "REFUNDED", "50.00");
        service.onChannelRefundSuccess("A20261031120000000001");
        verify(aftersaleMapper, never()).update(isNull(), any());
        verify(goodsApi, never()).restoreStock(anyLong(), anyInt());
    }

    @Test
    void 模拟支付订单退款_完成并在备注注明线下处理() {
        orderIs(OrderStatus.DELIVERED, null);
        Aftersale a = aftersale("REFUND", "APPLYING", "20.00");
        when(payService.refund(any(), any(), anyString()))
                .thenReturn(RefundResult.offline("模拟支付订单，需线下处理退款"));

        service.approve(9L, AS_ID, "同意");

        assertThat(a.getStatus()).isEqualTo("REFUNDED");
        assertThat(AftersaleServiceImpl.appendRemark("同意", "【需线下处理】x")).isEqualTo("同意 【需线下处理】x");
    }

    @Test
    void 渠道退款失败_保持退款中_备注只写一次() {
        Aftersale a = aftersale("REFUND", "REFUNDING", "20.00");
        a.setAuditRemark("同意 【退款失败：异常】");

        service.onChannelRefundFailed("A20261031120000000001", "异常");

        verify(aftersaleMapper, never()).update(isNull(), any());
        assertThat(a.getStatus()).isEqualTo("REFUNDING");
    }
}
