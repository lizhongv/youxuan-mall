package com.platform.cloud.mall.trade.order.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.marketing.api.CouponApi;
import com.platform.cloud.mall.marketing.api.SeckillApi;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.trade.MpTestSupport;
import com.platform.cloud.mall.trade.aftersale.entity.Aftersale;
import com.platform.cloud.mall.trade.aftersale.mapper.AftersaleMapper;
import com.platform.cloud.mall.trade.common.TradeSettings;
import com.platform.cloud.mall.trade.order.dto.OrderDeliverRequest;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.entity.OrderItem;
import com.platform.cloud.mall.trade.order.entity.OrderLog;
import com.platform.cloud.mall.trade.order.mapper.OrderItemMapper;
import com.platform.cloud.mall.trade.order.mapper.OrderLogMapper;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.order.service.OrderStateMachine;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
class OrderServiceImplTest {

    private static final Long MEMBER = 1001L;
    private static final Long ORDER_ID = 5001L;

    @Mock
    OrderMapper orderMapper;
    @Mock
    OrderItemMapper orderItemMapper;
    @Mock
    OrderLogMapper orderLogMapper;
    @Mock
    AftersaleMapper aftersaleMapper;
    @Mock
    GoodsApi goodsApi;
    @Mock
    CouponApi couponApi;
    @Mock
    SeckillApi seckillApi;
    @Mock
    MemberApi memberApi;
    @Mock
    TradeSettings settings;

    OrderServiceImpl service;

    @BeforeAll
    static void initMp() {
        MpTestSupport.init(Order.class, OrderItem.class, OrderLog.class, Aftersale.class);
    }

    @BeforeEach
    void setUp() {
        // 状态机用真实实现，验证条件更新与日志
        OrderStateMachine sm = new OrderStateMachine(orderMapper, orderLogMapper);
        service = new OrderServiceImpl(orderMapper, orderItemMapper, orderLogMapper, aftersaleMapper, sm,
                goodsApi, couponApi, seckillApi, memberApi, settings);
        when(orderMapper.update(isNull(), any())).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item(1L, 11L, 2), item(2L, 12L, 1)));
        when(settings.pointsPerYuan()).thenReturn(BigDecimal.ONE);
        when(settings.growthPerYuan()).thenReturn(new BigDecimal("0.5"));
        when(settings.autoReceiveDays()).thenReturn(7);
        when(aftersaleMapper.sumRefundedByOrder(anyLong())).thenReturn(BigDecimal.ZERO);
    }

    private static Order order(String status) {
        Order o = new Order();
        o.setId(ORDER_ID);
        o.setOrderNo("20261031120000123456");
        o.setMemberId(MEMBER);
        o.setStatus(status);
        o.setOrderType("NORMAL");
        o.setPayAmount(new BigDecimal("99.99"));
        o.setAdjustAmount(BigDecimal.ZERO);
        o.setFreightAmount(BigDecimal.ZERO);
        o.setPayExpireAt(LocalDateTime.now().minusMinutes(1));
        return o;
    }

    private static OrderItem item(Long id, Long skuId, int qty) {
        OrderItem i = new OrderItem();
        i.setId(id);
        i.setOrderId(ORDER_ID);
        i.setSkuId(skuId);
        i.setQuantity(qty);
        i.setPayAmount(new BigDecimal("50.00"));
        i.setTotalAmount(new BigDecimal("50.00"));
        i.setAftersaleStatus(OrderItem.AS_NONE);
        return i;
    }

    // ------------------------------------------------------------------ 取消回补

    @Test
    void 买家取消_回补库存_退券_写日志() {
        Order o = order("PENDING_PAY");
        o.setMemberCouponId(88L);
        when(orderMapper.selectOne(any())).thenReturn(o);

        service.memberCancel(MEMBER, ORDER_ID, "不想买了");

        verify(goodsApi).restoreStock(11L, 2);
        verify(goodsApi).restoreStock(12L, 1);
        verify(couponApi).giveBack(88L);
        verify(seckillApi, never()).release(anyLong(), anyLong(), anyInt());
        ArgumentCaptor<OrderLog> log = ArgumentCaptor.forClass(OrderLog.class);
        verify(orderLogMapper).insert(log.capture());
        assertThat(log.getValue().getFromStatus()).isEqualTo("PENDING_PAY");
        assertThat(log.getValue().getToStatus()).isEqualTo("CANCELLED");
        assertThat(log.getValue().getOperator()).isEqualTo("member:" + MEMBER);
        assertThat(log.getValue().getRemark()).isEqualTo("不想买了");
    }

    @Test
    void 取消秒杀单归还名额_活动ID取订单列() {
        Order o = order("PENDING_PAY");
        o.setOrderType("SECKILL");
        o.setSeckillId(77L);
        when(orderMapper.selectOne(any())).thenReturn(o);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item(1L, 11L, 2)));

        service.memberCancel(MEMBER, ORDER_ID, null);

        verify(seckillApi).release(77L, MEMBER, 2);
        verify(goodsApi).restoreStock(11L, 2);
    }

    @Test
    void 关单时秒杀名额在取消事务内直接归还_不延迟到提交后() {
        Order o = order("PENDING_PAY");
        o.setOrderType("SECKILL");
        o.setSeckillId(77L);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(o);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item(1L, 11L, 2)));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.closeExpired(ORDER_ID);
            // 事务尚未提交时 release（扣 DB sold、登记 Redis afterCommit）已经执行；
            // 订单服务自己不登记任何 afterCommit，否则 release 内部的 afterCommit 永远不会触发
            verify(seckillApi).release(77L, MEMBER, 2);
            assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 秒杀单缺少活动ID时不归还只记日志() {
        Order o = order("PENDING_PAY");
        o.setOrderType("SECKILL");
        when(orderMapper.selectOne(any())).thenReturn(o);

        service.memberCancel(MEMBER, ORDER_ID, null);

        verify(seckillApi, never()).release(anyLong(), anyLong(), anyInt());
    }

    @Test
    void 非待付款订单不能取消() {
        when(orderMapper.selectOne(any())).thenReturn(order("PENDING_DELIVERY"));

        assertThatThrownBy(() -> service.memberCancel(MEMBER, ORDER_ID, "x")).hasMessageContaining("仅待付款");
        verify(goodsApi, never()).restoreStock(anyLong(), anyInt());
    }

    @Test
    void 并发下状态已变化时取消失败且不回补() {
        when(orderMapper.selectOne(any())).thenReturn(order("PENDING_PAY"));
        when(orderMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.memberCancel(MEMBER, ORDER_ID, "x")).hasMessageContaining("状态已变化");
        verify(goodsApi, never()).restoreStock(anyLong(), anyInt());
        verify(orderLogMapper, never()).insert(any(OrderLog.class));
    }

    @Test
    void 他人订单视为不存在() {
        when(orderMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.memberDetail(MEMBER, ORDER_ID)).hasMessage("订单不存在");
    }

    @Test
    void 超时关单由系统执行() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order("PENDING_PAY"));

        service.closeExpired(ORDER_ID);

        ArgumentCaptor<OrderLog> log = ArgumentCaptor.forClass(OrderLog.class);
        verify(orderLogMapper).insert(log.capture());
        assertThat(log.getValue().getOperator()).isEqualTo("system");
        verify(goodsApi).restoreStock(11L, 2);
    }

    @Test
    void 未到期或已支付的订单不关闭() {
        Order notExpired = order("PENDING_PAY");
        notExpired.setPayExpireAt(LocalDateTime.now().plusMinutes(5));
        when(orderMapper.selectById(ORDER_ID)).thenReturn(notExpired);
        service.closeExpired(ORDER_ID);

        when(orderMapper.selectById(ORDER_ID)).thenReturn(order("PENDING_DELIVERY"));
        service.closeExpired(ORDER_ID);

        verify(orderMapper, never()).update(isNull(), any());
        verify(goodsApi, never()).restoreStock(anyLong(), anyInt());
    }

    // ------------------------------------------------------------------ 收货与奖励

    @Test
    void 确认收货按实付减已退款发放积分与成长值_向下取整() {
        when(orderMapper.selectOne(any())).thenReturn(order("DELIVERED"));
        when(aftersaleMapper.sumRefundedByOrder(ORDER_ID)).thenReturn(new BigDecimal("10.00"));

        service.memberReceive(MEMBER, ORDER_ID);

        // 99.99 − 10 = 89.99 → 积分 89，成长值 89.99 × 0.5 = 44.995 → 44
        verify(memberApi).changePoints(eq(MEMBER), eq(89), eq("ORDER_REWARD"), anyString(), anyString());
        verify(memberApi).addGrowth(eq(MEMBER), eq(44), eq("ORDER_REWARD"), anyString(), anyString());
        // 发放数量与基数同一条 UPDATE 记到订单上，供退款时按比例回收
        ArgumentCaptor<LambdaUpdateWrapper<Order>> w = updateCaptor();
        verify(orderMapper).update(isNull(), w.capture());
        assertThat(w.getValue().getSqlSet()).contains("reward_points", "reward_growth", "reward_base");
        assertThat(w.getValue().getParamNameValuePairs().values()).contains(89, 44, new BigDecimal("89.99"));
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<LambdaUpdateWrapper<Order>> updateCaptor() {
        return ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
    }

    @Test
    void 非待收货订单不能确认收货() {
        when(orderMapper.selectOne(any())).thenReturn(order("PENDING_DELIVERY"));
        assertThatThrownBy(() -> service.memberReceive(MEMBER, ORDER_ID)).hasMessageContaining("仅待收货");
        verify(memberApi, never()).changePoints(anyLong(), anyInt(), anyString(), anyString(), anyString());
    }

    @Test
    void 奖励计算向下取整() {
        assertThat(OrderServiceImpl.rewardOf(new BigDecimal("0.99"), BigDecimal.ONE)).isZero();
        assertThat(OrderServiceImpl.rewardOf(new BigDecimal("100.50"), new BigDecimal("2"))).isEqualTo(201);
        assertThat(OrderServiceImpl.rewardOf(new BigDecimal("100"), BigDecimal.ZERO)).isZero();
        assertThat(OrderServiceImpl.rewardOf(null, BigDecimal.ONE)).isZero();
    }

    // ------------------------------------------------------------------ 管理端

    @Test
    void 发货写入物流并设置自动收货时间() {
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(order("PENDING_DELIVERY"));
        when(orderItemMapper.selectCount(any())).thenReturn(0L);
        OrderDeliverRequest req = new OrderDeliverRequest();
        req.setDeliveryCompany("顺丰");
        req.setDeliveryNo("SF123");

        service.deliver(9L, ORDER_ID, req);

        ArgumentCaptor<OrderLog> log = ArgumentCaptor.forClass(OrderLog.class);
        verify(orderLogMapper).insert(log.capture());
        assertThat(log.getValue().getToStatus()).isEqualTo("DELIVERED");
        assertThat(log.getValue().getOperator()).isEqualTo("admin:9");
        // 先锁订单行再检查售后，与售后申请串行
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(orderMapper, orderItemMapper);
        inOrder.verify(orderMapper).selectByIdForUpdate(ORDER_ID);
        inOrder.verify(orderItemMapper).selectCount(any());
        verify(orderMapper, never()).selectById(any());
    }

    @Test
    void 发货时订单不存在() {
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(null);
        OrderDeliverRequest req = new OrderDeliverRequest();
        req.setDeliveryCompany("顺丰");
        req.setDeliveryNo("SF123");
        assertThatThrownBy(() -> service.deliver(9L, ORDER_ID, req)).hasMessage("订单不存在");
    }

    @Test
    void 有处理中售后时不能发货() {
        when(orderMapper.selectByIdForUpdate(ORDER_ID)).thenReturn(order("PENDING_DELIVERY"));
        when(orderItemMapper.selectCount(any())).thenReturn(1L);
        OrderDeliverRequest req = new OrderDeliverRequest();
        req.setDeliveryCompany("顺丰");
        req.setDeliveryNo("SF123");

        assertThatThrownBy(() -> service.deliver(9L, ORDER_ID, req)).hasMessageContaining("售后");
    }

    @Test
    void 改价后实付不得低于0点01() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order("PENDING_PAY"));
        assertThatThrownBy(() -> service.adjustPrice(9L, ORDER_ID, new BigDecimal("-99.99")))
                .hasMessageContaining("0.01");
        verify(orderMapper, never()).update(isNull(), any());
    }

    @Test
    void 改价下限为运费加每件0点01() {
        Order o = order("PENDING_PAY");
        o.setPayAmount(new BigDecimal("110.00"));
        o.setFreightAmount(new BigDecimal("10.00"));
        when(orderMapper.selectById(ORDER_ID)).thenReturn(o);
        // 明细共 3 件 → 下限 10.03；改到 10.02 被拒
        assertThatThrownBy(() -> service.adjustPrice(9L, ORDER_ID, new BigDecimal("-99.98")))
                .hasMessageContaining("10.03");
        verify(orderMapper, never()).update(isNull(), any());

        // 恰好 10.03 可以，每件商品至少分到 0.01
        service.adjustPrice(9L, ORDER_ID, new BigDecimal("-99.97"));
        verify(orderMapper).update(isNull(), any());
    }

    @Test
    void 改价重新分摊_每行预留每件0点01() {
        Order o = order("PENDING_PAY");
        o.setFreightAmount(new BigDecimal("10.00"));
        OrderItem big = item(1L, 11L, 1);
        big.setPayAmount(new BigDecimal("1000.00"));
        OrderItem tiny = item(2L, 12L, 2);
        tiny.setPayAmount(new BigDecimal("0.10"));

        java.util.Map<Long, BigDecimal> r = OrderServiceImpl.allocateItems(o, List.of(big, tiny),
                new BigDecimal("10.05"));

        // 商品部分 0.05：先各预留 0.01 × 件数（0.01 + 0.02），剩 0.02 按比例几乎全给大额行
        assertThat(r.get(2L)).isGreaterThanOrEqualTo(new BigDecimal("0.02"));
        assertThat(r.get(1L).add(r.get(2L))).isEqualByComparingTo("0.05");
    }

    @Test
    void 改价条件带读到的应付金额_并发改价或支付时失败() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order("PENDING_PAY"));
        when(orderMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.adjustPrice(9L, ORDER_ID, new BigDecimal("-9.99")))
                .hasMessageContaining("已变化");
        ArgumentCaptor<LambdaUpdateWrapper<Order>> w = updateCaptor();
        verify(orderMapper).update(isNull(), w.capture());
        assertThat(w.getValue().getSqlSegment()).contains("pay_amount");
        assertThat(w.getValue().getParamNameValuePairs().values()).contains(new BigDecimal("99.99"));
    }

    @Test
    void 自动收货查询排除售后中订单并按id游标分页() {
        when(orderMapper.selectPage(any(), any())).thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

        service.findAutoReceivable(500L, 200);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>> w =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(orderMapper).selectPage(any(), w.capture());
        String sql = w.getValue().getCustomSqlSegment();
        assertThat(sql).contains("NOT EXISTS").contains("aftersale_status = 'APPLYING'").contains("id >")
                .contains("ORDER BY id ASC");
        assertThat(w.getValue().getParamNameValuePairs().values()).contains(500L);
    }

    @Test
    void 关单查询按id游标分页_首页不带游标() {
        when(orderMapper.selectPage(any(), any())).thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

        service.findExpiredUnpaid(null, 200);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>> w =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(orderMapper).selectPage(any(), w.capture());
        assertThat(w.getValue().getCustomSqlSegment()).doesNotContain("id >").contains("ORDER BY id ASC");
    }

    @Test
    void 改价成功并重新分摊明细实付() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order("PENDING_PAY"));

        service.adjustPrice(9L, ORDER_ID, new BigDecimal("-9.99"));

        // 订单 1 次 + 明细 2 次
        verify(orderMapper).update(isNull(), any());
        verify(orderItemMapper, org.mockito.Mockito.times(2)).update(isNull(), any());
        ArgumentCaptor<OrderLog> log = ArgumentCaptor.forClass(OrderLog.class);
        verify(orderLogMapper).insert(log.capture());
        assertThat(log.getValue().getRemark()).contains("99.99 → 90.00");
    }

    @Test
    void 后台只能取消待付款订单() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order("PENDING_DELIVERY"));
        assertThatThrownBy(() -> service.adminCancel(9L, ORDER_ID, "x"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("仅待付款");
    }

    @Test
    void 状态计数包含全部状态() {
        when(orderMapper.countByStatus()).thenReturn(List.of());
        assertThat(service.statusCount()).containsOnlyKeys("PENDING_PAY", "PENDING_DELIVERY", "DELIVERED",
                "COMPLETED", "CANCELLED").allSatisfy((k, v) -> assertThat(v).isZero());
    }
}
