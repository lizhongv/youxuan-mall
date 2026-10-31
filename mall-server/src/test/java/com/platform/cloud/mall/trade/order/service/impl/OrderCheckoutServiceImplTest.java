package com.platform.cloud.mall.trade.order.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.goods.api.GoodsApi.SkuSnapshot;
import com.platform.cloud.mall.marketing.api.CouponApi;
import com.platform.cloud.mall.marketing.api.CouponApi.CouponOption;
import com.platform.cloud.mall.marketing.api.SeckillApi;
import com.platform.cloud.mall.marketing.api.SeckillApi.SeckillInfo;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.member.api.MemberApi.AddressSnapshot;
import com.platform.cloud.mall.member.api.MemberApi.MemberBrief;
import com.platform.cloud.mall.trade.MpTestSupport;
import com.platform.cloud.mall.trade.cart.entity.Cart;
import com.platform.cloud.mall.trade.cart.mapper.CartMapper;
import com.platform.cloud.mall.trade.common.TradeSettings;
import com.platform.cloud.mall.trade.freight.service.FreightQuoteService;
import com.platform.cloud.mall.trade.freight.support.FreightCalculator;
import com.platform.cloud.mall.trade.order.service.PriceCalculator;
import com.platform.cloud.mall.trade.order.dto.OrderSubmitRequest;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.entity.OrderItem;
import com.platform.cloud.mall.trade.order.entity.OrderLog;
import com.platform.cloud.mall.trade.order.mapper.OrderItemMapper;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.order.service.OrderStateMachine;
import com.platform.cloud.mall.trade.order.service.PriceCalculator.FreightRule;
import com.platform.cloud.mall.trade.order.vo.OrderConfirmVO;
import com.platform.cloud.mall.trade.order.vo.OrderSubmitVO;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderCheckoutServiceImplTest {

    private static final Long MEMBER = 1001L;
    private static final Long ADDRESS = 2001L;
    private static final Long SKU_A = 11L;
    private static final Long SKU_B = 12L;

    @Mock
    GoodsApi goodsApi;
    @Mock
    MemberApi memberApi;
    @Mock
    CouponApi couponApi;
    @Mock
    SeckillApi seckillApi;
    @Mock
    TradeSettings settings;
    @Mock
    CartMapper cartMapper;
    @Mock
    OrderMapper orderMapper;
    @Mock
    OrderItemMapper orderItemMapper;
    @Mock
    OrderStateMachine stateMachine;
    @Mock
    FreightQuoteService freightQuote;
    @InjectMocks
    OrderCheckoutServiceImpl service;

    @BeforeAll
    static void initMp() {
        MpTestSupport.init(Order.class, OrderItem.class, Cart.class, OrderLog.class);
    }

    @BeforeEach
    void setUp() {
        when(memberApi.getMember(MEMBER)).thenReturn(member(new BigDecimal("0.90")));
        when(memberApi.getAddress(MEMBER, ADDRESS)).thenReturn(
                new AddressSnapshot("张三", "13800000000", "广东省", "深圳市", "南山区", "科技园 1 号"));
        when(settings.freightRule()).thenReturn(new FreightRule(new BigDecimal("10"), new BigDecimal("99")));
        when(settings.payTimeoutMinutes()).thenReturn(30);
        // 无运费模板时的旧规则：按折后合计判断满 99 包邮，否则 10 元（模板计算本身见 FreightCalculatorTest）
        FreightRule rule = new FreightRule(new BigDecimal("10"), new BigDecimal("99"));
        when(freightQuote.quote(any(), any())).thenAnswer(inv -> {
            List<FreightCalculator.Item> items = inv.getArgument(1);
            BigDecimal total = items.stream().map(FreightCalculator.Item::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return PriceCalculator.freight(total, rule);
        });
        when(goodsApi.getSkus(any())).thenReturn(Map.of(
                SKU_A, sku(SKU_A, "100.00", 10, true),
                SKU_B, sku(SKU_B, "50.00", 10, true)));
    }

    private static MemberBrief member(BigDecimal discount) {
        return new MemberBrief(MEMBER, "138****0000", "张三", "", 1L, "黄金会员", discount,
                new BigDecimal("500.00"), 0, 1);
    }

    private static SkuSnapshot sku(Long id, String price, int stock, boolean onSale) {
        return new SkuSnapshot(id, id * 100, 9L, "商品" + id, "pic" + id, "规格" + id, new BigDecimal(price), stock,
                onSale);
    }

    private static OrderSubmitRequest buyNow(Long skuId, int qty) {
        OrderSubmitRequest req = new OrderSubmitRequest();
        req.setSource(OrderSubmitRequest.SOURCE_BUY_NOW);
        OrderSubmitRequest.Item it = new OrderSubmitRequest.Item();
        it.setSkuId(skuId);
        it.setQuantity(qty);
        req.setItems(List.of(it));
        req.setAddressId(ADDRESS);
        return req;
    }

    private static SeckillInfo seckillInfo(LocalDateTime start, LocalDateTime end, int limit) {
        return new SeckillInfo(77L, SKU_A * 100, SKU_A, new BigDecimal("9.90"), 100, 0, limit, start, end, true);
    }

    // ------------------------------------------------------------------ 校验失败

    @Test
    void 库存不足时不扣库存不建单() {
        when(goodsApi.getSkus(any())).thenReturn(Map.of(SKU_A, sku(SKU_A, "100.00", 1, true)));

        assertThatThrownBy(() -> service.submit(MEMBER, buyNow(SKU_A, 2)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("库存不足");
        verify(goodsApi, never()).deductStock(anyLong(), anyInt());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void 并发扣库存失败时整单失败不建单() {
        doThrow(new BusinessException("商品11 库存不足")).when(goodsApi).deductStock(SKU_A, 2);

        assertThatThrownBy(() -> service.submit(MEMBER, buyNow(SKU_A, 2)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("库存不足");
        verify(orderMapper, never()).insert(any(Order.class));
        verify(couponApi, never()).use(anyLong(), anyLong(), anyLong());
    }

    @Test
    void 使用他人地址被会员领域拒绝() {
        when(memberApi.getAddress(MEMBER, 9999L)).thenThrow(new BusinessException("收货地址不存在"));
        OrderSubmitRequest req = buyNow(SKU_A, 1);
        req.setAddressId(9999L);

        assertThatThrownBy(() -> service.submit(MEMBER, req))
                .isInstanceOf(BusinessException.class).hasMessage("收货地址不存在");
        verify(goodsApi, never()).deductStock(anyLong(), anyInt());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void 未选地址不能下单() {
        OrderSubmitRequest req = buyNow(SKU_A, 1);
        req.setAddressId(null);
        assertThatThrownBy(() -> service.submit(MEMBER, req)).hasMessage("请选择收货地址");
    }

    @Test
    void 下架商品不能下单() {
        when(goodsApi.getSkus(any())).thenReturn(Map.of(SKU_A, sku(SKU_A, "100.00", 10, false)));
        assertThatThrownBy(() -> service.submit(MEMBER, buyNow(SKU_A, 1))).hasMessageContaining("已下架");
    }

    @Test
    void 不存在的SKU不能下单() {
        when(goodsApi.getSkus(any())).thenReturn(Map.of());
        assertThatThrownBy(() -> service.submit(MEMBER, buyNow(SKU_A, 1))).hasMessageContaining("不存在");
    }

    @Test
    void 同一SKU合并后超过99件被拒绝() {
        OrderSubmitRequest req = buyNow(SKU_A, 60);
        OrderSubmitRequest.Item more = new OrderSubmitRequest.Item();
        more.setSkuId(SKU_A);
        more.setQuantity(40);
        req.setItems(List.of(req.getItems().get(0), more));
        assertThatThrownBy(() -> service.submit(MEMBER, req)).hasMessageContaining("99");
    }

    @Test
    void 购物车项不属于当前会员时被拒绝() {
        OrderSubmitRequest req = new OrderSubmitRequest();
        req.setSource(OrderSubmitRequest.SOURCE_CART);
        req.setCartIds(List.of(1L, 2L));
        req.setAddressId(ADDRESS);
        // 带 member_id 条件只查到 1 条
        when(cartMapper.selectList(any())).thenReturn(List.of(cart(1L, SKU_A, 1)));

        assertThatThrownBy(() -> service.submit(MEMBER, req)).hasMessageContaining("购物车商品已变化");
    }

    @Test
    void 停用会员不能下单() {
        when(memberApi.getMember(MEMBER)).thenReturn(new MemberBrief(MEMBER, "", "", "", 1L, "", BigDecimal.ONE,
                BigDecimal.ZERO, 0, 0));
        assertThatThrownBy(() -> service.submit(MEMBER, buyNow(SKU_A, 1))).hasMessageContaining("停用");
    }

    // ------------------------------------------------------------------ 秒杀

    @Test
    void 秒杀每单只能一个SKU() {
        OrderSubmitRequest req = buyNow(SKU_A, 1);
        OrderSubmitRequest.Item b = new OrderSubmitRequest.Item();
        b.setSkuId(SKU_B);
        b.setQuantity(1);
        req.setItems(List.of(req.getItems().get(0), b));
        req.setSeckillId(77L);

        assertThatThrownBy(() -> service.submit(MEMBER, req)).hasMessageContaining("只能购买一个商品");
        verify(seckillApi, never()).acquire(anyLong(), anyLong(), anyInt(), anyInt());
    }

    @Test
    void 秒杀不能用券() {
        when(seckillApi.get(77L)).thenReturn(seckillInfo(LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1), 2));
        OrderSubmitRequest req = buyNow(SKU_A, 1);
        req.setSeckillId(77L);
        req.setMemberCouponId(5L);

        assertThatThrownBy(() -> service.submit(MEMBER, req)).hasMessageContaining("不可使用优惠券");
        verify(seckillApi, never()).acquire(anyLong(), anyLong(), anyInt(), anyInt());
    }

    @Test
    void 秒杀未开始() {
        when(seckillApi.get(77L)).thenReturn(seckillInfo(LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2), 2));
        OrderSubmitRequest req = buyNow(SKU_A, 1);
        req.setSeckillId(77L);
        assertThatThrownBy(() -> service.submit(MEMBER, req)).hasMessageContaining("尚未开始");
    }

    @Test
    void 秒杀已结束() {
        when(seckillApi.get(77L)).thenReturn(seckillInfo(LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1), 2));
        OrderSubmitRequest req = buyNow(SKU_A, 1);
        req.setSeckillId(77L);
        assertThatThrownBy(() -> service.submit(MEMBER, req)).hasMessageContaining("已结束");
    }

    @Test
    void 秒杀超出限购() {
        when(seckillApi.get(77L)).thenReturn(seckillInfo(LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1), 2));
        OrderSubmitRequest req = buyNow(SKU_A, 3);
        req.setSeckillId(77L);
        assertThatThrownBy(() -> service.submit(MEMBER, req)).hasMessageContaining("限购 2");
    }

    @Test
    void 秒杀SKU与活动不匹配() {
        when(seckillApi.get(77L)).thenReturn(seckillInfo(LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1), 2));
        OrderSubmitRequest req = buyNow(SKU_B, 1);
        req.setSeckillId(77L);
        assertThatThrownBy(() -> service.submit(MEMBER, req)).hasMessageContaining("不匹配");
    }

    @Test
    void 秒杀成功_秒杀价_不打折_先抢名额再扣库存() {
        when(seckillApi.get(77L)).thenReturn(seckillInfo(LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1), 2));
        OrderSubmitRequest req = buyNow(SKU_A, 2);
        req.setSeckillId(77L);
        when(orderMapper.sumSeckillPurchased(77L, MEMBER)).thenReturn(0);

        service.submit(MEMBER, req);

        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(order.capture());
        assertThat(order.getValue().getOrderType()).isEqualTo("SECKILL");
        assertThat(order.getValue().getGoodsAmount()).isEqualByComparingTo("19.80");
        assertThat(order.getValue().getDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(order.getValue().getPayAmount()).isEqualByComparingTo("29.80");
        assertThat(order.getValue().getSeckillId()).isEqualTo(77L);
        verify(seckillApi).acquire(77L, MEMBER, 2, 0);
        verify(goodsApi).deductStock(SKU_A, 2);
        ArgumentCaptor<String> remark = ArgumentCaptor.forClass(String.class);
        verify(stateMachine).log(anyLong(), isNull(), eq("PENDING_PAY"), eq("member:" + MEMBER), remark.capture());
        assertThat(remark.getValue()).contains("77");
    }

    @Test
    void 秒杀个人已购以订单为准传给营销领域() {
        when(seckillApi.get(77L)).thenReturn(seckillInfo(LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1), 2));
        when(orderMapper.sumSeckillPurchased(77L, MEMBER)).thenReturn(1);
        doThrow(new BusinessException("每人限购 2 件，您已达到限购数量")).when(seckillApi).acquire(77L, MEMBER, 2, 1);
        OrderSubmitRequest req = buyNow(SKU_A, 2);
        req.setSeckillId(77L);

        assertThatThrownBy(() -> service.submit(MEMBER, req)).hasMessageContaining("限购");
        verify(goodsApi, never()).deductStock(anyLong(), anyInt());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void 秒杀下单回滚时checkout不再登记归还_由acquire内部补偿() {
        when(seckillApi.get(77L)).thenReturn(seckillInfo(LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1), 2));
        doThrow(new BusinessException("库存不足")).when(goodsApi).deductStock(SKU_A, 1);
        OrderSubmitRequest req = buyNow(SKU_A, 1);
        req.setSeckillId(77L);

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThatThrownBy(() -> service.submit(MEMBER, req)).hasMessage("库存不足");
            // acquire 是 mock，未登记任何回调；checkout 自己也不能登记（否则回滚时会重复归还）
            assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
        verify(seckillApi).acquire(77L, MEMBER, 1, 0);
        verify(seckillApi, never()).release(anyLong(), anyLong(), anyInt());
    }

    @Test
    void 多SKU按skuId升序扣库存_避免死锁() {
        OrderSubmitRequest req = new OrderSubmitRequest();
        req.setSource(OrderSubmitRequest.SOURCE_CART);
        req.setCartIds(List.of(1L, 2L));
        req.setAddressId(ADDRESS);
        // 购物车顺序是 B 在前
        when(cartMapper.selectList(any())).thenReturn(List.of(cart(1L, SKU_B, 1), cart(2L, SKU_A, 1)));

        service.submit(MEMBER, req);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(goodsApi);
        inOrder.verify(goodsApi).deductStock(SKU_A, 1);
        inOrder.verify(goodsApi).deductStock(SKU_B, 1);
    }

    @Test
    void 运费按收货省份与商品运费模板报价() {
        when(goodsApi.getShipping(any())).thenReturn(Map.of(SKU_A,
                new GoodsApi.ShippingInfo(SKU_A, 300L, new BigDecimal("1.500"))));
        org.mockito.Mockito.doReturn(new BigDecimal("12.00")).when(freightQuote).quote(eq("广东省"), any());

        service.submit(MEMBER, buyNow(SKU_A, 1));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FreightCalculator.Item>> items = ArgumentCaptor.forClass(List.class);
        verify(freightQuote).quote(eq("广东省"), items.capture());
        FreightCalculator.Item it = items.getValue().get(0);
        assertThat(it.templateId()).isEqualTo(300L);
        assertThat(it.unitWeight()).isEqualByComparingTo("1.5");
        // 折后金额 100 × 0.9
        assertThat(it.amount()).isEqualByComparingTo("90.00");
        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(order.capture());
        assertThat(order.getValue().getFreightAmount()).isEqualByComparingTo("12.00");
        assertThat(order.getValue().getPayAmount()).isEqualByComparingTo("102.00");
    }

    @Test
    void 抢名额失败无需归还() {
        when(seckillApi.get(77L)).thenReturn(seckillInfo(LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1), 2));
        doThrow(new BusinessException("已抢光")).when(seckillApi).acquire(77L, MEMBER, 1, 0);
        OrderSubmitRequest req = buyNow(SKU_A, 1);
        req.setSeckillId(77L);

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThatThrownBy(() -> service.submit(MEMBER, req)).hasMessage("已抢光");
            assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
        verify(goodsApi, never()).deductStock(anyLong(), anyInt());
    }

    // ------------------------------------------------------------------ 成功路径

    private static Cart cart(Long id, Long skuId, int qty) {
        Cart c = new Cart();
        c.setId(id);
        c.setMemberId(MEMBER);
        c.setSkuId(skuId);
        c.setQuantity(qty);
        return c;
    }

    @Test
    void 购物车下单成功_折扣_用券_扣库存_核销券_删购物车_写日志() {
        OrderSubmitRequest req = new OrderSubmitRequest();
        req.setSource(OrderSubmitRequest.SOURCE_CART);
        req.setCartIds(List.of(1L, 2L));
        req.setAddressId(ADDRESS);
        req.setMemberCouponId(5L);
        req.setRemark(" 尽快发货 ");
        when(cartMapper.selectList(any())).thenReturn(List.of(cart(1L, SKU_A, 1), cart(2L, SKU_B, 2)));
        when(couponApi.calculate(eq(MEMBER), eq(5L), any())).thenReturn(new BigDecimal("18.00"));

        OrderSubmitVO vo = service.submit(MEMBER, req);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(captor.capture());
        Order o = captor.getValue();
        // 商品 100 + 50×2 = 200；九折 → 180，折扣 20；券 18 → 162；折后 ≥ 99 包邮
        assertThat(o.getGoodsAmount()).isEqualByComparingTo("200.00");
        assertThat(o.getDiscountAmount()).isEqualByComparingTo("20.00");
        assertThat(o.getCouponAmount()).isEqualByComparingTo("18.00");
        assertThat(o.getFreightAmount()).isEqualByComparingTo("0.00");
        assertThat(o.getPayAmount()).isEqualByComparingTo("162.00");
        assertThat(o.getStatus()).isEqualTo("PENDING_PAY");
        assertThat(o.getOrderNo()).matches("\\d{20}");
        assertThat(o.getReceiverName()).isEqualTo("张三");
        assertThat(o.getRemark()).isEqualTo("尽快发货");
        assertThat(o.getPayExpireAt()).isAfter(LocalDateTime.now().plusMinutes(29));
        assertThat(vo.orderId()).isEqualTo(o.getId()).isNotNull();

        ArgumentCaptor<OrderItem> items = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemMapper, times(2)).insert(items.capture());
        // 券按折后金额 90 : 90 分摊 → 各 9
        assertThat(items.getAllValues()).extracting(OrderItem::getPayAmount)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("81.00"), new BigDecimal("81.00"));

        verify(goodsApi).deductStock(SKU_A, 1);
        verify(goodsApi).deductStock(SKU_B, 2);
        verify(couponApi).use(MEMBER, 5L, o.getId());
        verify(cartMapper).delete(any());
        verify(stateMachine).log(eq(o.getId()), isNull(), eq("PENDING_PAY"), eq("member:" + MEMBER), anyString());
        verify(seckillApi, never()).acquire(anyLong(), anyLong(), anyInt(), anyInt());
    }

    @Test
    void 订单号冲突时换号重试() {
        when(orderMapper.insert(any(Order.class)))
                .thenThrow(new DuplicateKeyException("uk_order_no"))
                .thenReturn(1);

        service.submit(MEMBER, buyNow(SKU_A, 1));

        verify(orderMapper, times(2)).insert(any(Order.class));
    }

    @Test
    void 立即购买不删购物车_memberCouponId为0不用券() {
        OrderSubmitRequest req = buyNow(SKU_A, 1);
        req.setMemberCouponId(0L);

        service.submit(MEMBER, req);

        verify(cartMapper, never()).delete(any());
        verify(couponApi, never()).calculate(anyLong(), anyLong(), any());
        verify(couponApi, never()).use(anyLong(), anyLong(), anyLong());
    }

    // ------------------------------------------------------------------ 结算预览

    private static CouponOption option(Long id, boolean usable, String discount) {
        return new CouponOption(id, "券" + id, "FULL_REDUCTION", BigDecimal.ZERO, new BigDecimal(discount),
                BigDecimal.ONE, LocalDateTime.now().plusDays(1), usable, new BigDecimal(discount),
                usable ? null : "未满足门槛");
    }

    @Test
    void 预览未传券时自动选可用且优惠最大的券() {
        when(couponApi.listForCheckout(eq(MEMBER), any())).thenReturn(List.of(
                option(1L, true, "5.00"), option(2L, true, "8.00"), option(3L, false, "50.00")));
        when(couponApi.calculate(eq(MEMBER), eq(2L), any())).thenReturn(new BigDecimal("8.00"));
        OrderSubmitRequest req = buyNow(SKU_A, 1);
        req.setAddressId(null);

        OrderConfirmVO vo = service.confirm(MEMBER, req);

        assertThat(vo.getMemberCouponId()).isEqualTo(2L);
        assertThat(vo.getCouponAmount()).isEqualByComparingTo("8.00");
        // 100 × 0.9 = 90 < 99 → 运费 10；90 − 8 + 10 = 92
        assertThat(vo.getPayAmount()).isEqualByComparingTo("92.00");
        assertThat(vo.getAddress()).isNull();
        assertThat(vo.getCoupons()).hasSize(3);
        assertThat(vo.getBalance()).isEqualByComparingTo("500.00");
        verify(orderMapper, never()).insert(any(Order.class));
        verify(goodsApi, never()).deductStock(anyLong(), anyInt());
    }

    @Test
    void 预览传0表示不用券() {
        when(couponApi.listForCheckout(eq(MEMBER), any())).thenReturn(List.of(option(1L, true, "5.00")));
        OrderSubmitRequest req = buyNow(SKU_A, 1);
        req.setMemberCouponId(0L);

        OrderConfirmVO vo = service.confirm(MEMBER, req);

        assertThat(vo.getMemberCouponId()).isNull();
        assertThat(vo.getCouponAmount()).isEqualByComparingTo("0.00");
        assertThat(vo.getAddress().getName()).isEqualTo("张三");
        verify(couponApi, never()).calculate(anyLong(), anyLong(), any());
    }

    @Test
    void 预览秒杀商品不查券() {
        when(seckillApi.get(77L)).thenReturn(seckillInfo(LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1), 2));
        OrderSubmitRequest req = buyNow(SKU_A, 1);
        req.setSeckillId(77L);

        OrderConfirmVO vo = service.confirm(MEMBER, req);

        assertThat(vo.getCoupons()).isEmpty();
        assertThat(vo.getItems().get(0).getPrice()).isEqualByComparingTo("9.90");
        verify(couponApi, never()).listForCheckout(anyLong(), any());
        verify(seckillApi, never()).acquire(anyLong(), anyLong(), anyInt(), anyInt());
    }
}
