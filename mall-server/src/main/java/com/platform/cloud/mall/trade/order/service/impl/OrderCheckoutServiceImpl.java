package com.platform.cloud.mall.trade.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.goods.api.GoodsApi.GoodsLine;
import com.platform.cloud.mall.goods.api.GoodsApi.ShippingInfo;
import com.platform.cloud.mall.goods.api.GoodsApi.SkuSnapshot;
import com.platform.cloud.mall.marketing.api.CouponApi;
import com.platform.cloud.mall.marketing.api.CouponApi.CouponOption;
import com.platform.cloud.mall.marketing.api.SeckillApi;
import com.platform.cloud.mall.marketing.api.SeckillApi.SeckillInfo;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.member.api.MemberApi.AddressRef;
import com.platform.cloud.mall.member.api.MemberApi.AddressSnapshot;
import com.platform.cloud.mall.member.api.MemberApi.MemberBrief;
import com.platform.cloud.mall.trade.cart.entity.Cart;
import com.platform.cloud.mall.trade.cart.mapper.CartMapper;
import com.platform.cloud.mall.trade.common.NoGenerator;
import com.platform.cloud.mall.trade.common.Operators;
import com.platform.cloud.mall.trade.common.TradeDefaults;
import com.platform.cloud.mall.trade.common.TradeSettings;
import com.platform.cloud.mall.trade.freight.service.FreightQuoteService;
import com.platform.cloud.mall.trade.freight.support.FreightCalculator;
import com.platform.cloud.mall.trade.order.dto.OrderSubmitRequest;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.entity.OrderItem;
import com.platform.cloud.mall.trade.order.enums.OrderStatus;
import com.platform.cloud.mall.trade.order.enums.OrderType;
import com.platform.cloud.mall.trade.order.mapper.OrderItemMapper;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.order.service.OrderCheckoutService;
import com.platform.cloud.mall.trade.order.service.OrderStateMachine;
import com.platform.cloud.mall.trade.order.service.PriceCalculator;
import com.platform.cloud.mall.trade.order.service.PriceCalculator.Line;
import com.platform.cloud.mall.trade.order.service.PriceCalculator.LineResult;
import com.platform.cloud.mall.trade.order.service.PriceCalculator.Result;
import com.platform.cloud.mall.trade.order.vo.OrderConfirmVO;
import com.platform.cloud.mall.trade.order.vo.OrderSubmitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 结算与下单。
 * <p>
 * confirm 与 submit 共用 {@link #prepare} 与 {@link PriceCalculator}，保证预览金额与实际下单金额一致；
 * submit 在此基础上再做强校验（地址必填、券可用、秒杀名额、条件扣库存）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCheckoutServiceImpl implements OrderCheckoutService {

    /** 订单号碰撞时的重试次数：同一秒 100 万种组合，3 次仍冲突说明系统异常，直接失败 */
    private static final int ORDER_NO_RETRY = 3;

    private final GoodsApi goodsApi;
    private final MemberApi memberApi;
    private final CouponApi couponApi;
    private final SeckillApi seckillApi;
    private final TradeSettings settings;
    private final CartMapper cartMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStateMachine stateMachine;
    private final FreightQuoteService freightQuote;

    @Override
    public OrderConfirmVO confirm(Long memberId, OrderSubmitRequest req) {
        MemberBrief member = requireMember(memberId);
        Checkout c = prepare(memberId, req);
        List<GoodsLine> goodsLines = PriceCalculator.toGoodsLines(c.lines(), member.discount());

        Long couponId = null;
        BigDecimal couponAmount = BigDecimal.ZERO;
        List<CouponOption> options = List.of();
        // 秒杀订单不可用券，直接不查券列表
        if (c.seckill() == null) {
            options = couponApi.listForCheckout(memberId, goodsLines);
            if (req.getMemberCouponId() == null) {
                couponId = bestCoupon(options);
            } else if (req.getMemberCouponId() != 0L) {
                couponId = req.getMemberCouponId();
            }
            if (couponId != null) {
                couponAmount = couponApi.calculate(memberId, couponId, goodsLines);
            }
        }
        // 未传地址时预选默认地址（无默认取最近修改的），结算页首次进入就能显示地址与准确运费；
        // 会员没有地址时 address=null，运费按默认地区估算。提交订单仍要求显式传 addressId。
        Long addressId = req.getAddressId();
        AddressSnapshot address;
        if (addressId != null) {
            address = requireAddress(memberId, addressId);
        } else {
            AddressRef preferred = memberApi.getPreferredAddress(memberId);
            addressId = preferred == null ? null : preferred.id();
            address = preferred == null ? null : preferred.snapshot();
        }
        BigDecimal freight = freightOf(c, goodsLines, address == null ? null : address.province());
        Result r = PriceCalculator.calculateWithFreight(c.lines(), member.discount(), couponAmount, freight);

        OrderConfirmVO vo = new OrderConfirmVO();
        List<OrderConfirmVO.Item> items = new ArrayList<>();
        for (LineResult lr : r.lines()) {
            SkuSnapshot s = c.skus().get(lr.line().skuId());
            OrderConfirmVO.Item it = new OrderConfirmVO.Item();
            it.setGoodsId(s.goodsId());
            it.setSkuId(s.skuId());
            it.setGoodsName(s.goodsName());
            it.setPic(s.pic());
            it.setSpecText(s.specText());
            it.setPrice(lr.line().price());
            it.setQuantity(lr.line().quantity());
            it.setTotalAmount(lr.totalAmount());
            items.add(it);
        }
        vo.setItems(items);
        if (address != null) {
            vo.setAddress(toAddressVO(addressId, address));
        }
        vo.setGoodsAmount(r.goodsAmount());
        vo.setDiscountAmount(r.discountAmount());
        vo.setCouponAmount(r.couponAmount());
        vo.setFreightAmount(r.freightAmount());
        vo.setPayAmount(r.payAmount());
        vo.setMemberCouponId(couponId);
        vo.setCoupons(options);
        vo.setBalance(member.balance());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submit(Long memberId, OrderSubmitRequest req) {
        if (req.getAddressId() == null) {
            throw new BusinessException("请选择收货地址");
        }
        MemberBrief member = requireMember(memberId);
        // 地址由会员领域按 memberId 校验归属，防止用他人地址下单（越权读取他人收货信息）
        AddressSnapshot address = requireAddress(memberId, req.getAddressId());
        Checkout c = prepare(memberId, req);

        Long couponId = req.getMemberCouponId() == null || req.getMemberCouponId() == 0L
                ? null : req.getMemberCouponId();
        if (c.seckill() != null && couponId != null) {
            throw new BusinessException("秒杀订单不可使用优惠券");
        }
        List<GoodsLine> goodsLines = PriceCalculator.toGoodsLines(c.lines(), member.discount());
        BigDecimal couponAmount = couponId == null ? BigDecimal.ZERO
                : couponApi.calculate(memberId, couponId, goodsLines);
        BigDecimal freight = freightOf(c, goodsLines, address.province());
        Result r = PriceCalculator.calculateWithFreight(c.lines(), member.discount(), couponAmount, freight);

        // 秒杀：先在 Redis 原子抢名额，挡掉绝大多数无效请求再碰数据库。
        // 回滚补偿由 acquire 内部登记（sold 随事务回滚、Redis 名额回滚后补回），这里不能再登记 release，
        // 否则回滚时会归还两次，且 release 的 decreaseSold 会在已回滚的连接上执行。
        // 个人已购数以订单为准传入，防止 Redis 个人计数丢失后限购失效。
        if (c.seckill() != null) {
            SeckillInfo sk = c.seckill();
            int qty = c.lines().get(0).quantity();
            seckillApi.acquire(sk.id(), memberId, qty, orderMapper.sumSeckillPurchased(sk.id(), memberId));
        }
        // 条件扣减真实库存（WHERE stock >= ?），任一失败整单回滚。
        // 按 skuId 升序加行锁：两个订单包含相同的几个 SKU 时加锁顺序一致，避免互相等待形成死锁
        List<Line> byId = new ArrayList<>(c.lines());
        byId.sort(Comparator.comparing(Line::skuId));
        for (Line l : byId) {
            goodsApi.deductStock(l.skuId(), l.quantity());
        }

        LocalDateTime now = LocalDateTime.now();
        Order order = buildOrder(memberId, req, c, r, address, couponId, now);
        insertWithUniqueNo(order);
        for (LineResult lr : r.lines()) {
            orderItemMapper.insert(buildItem(order, c.skus().get(lr.line().skuId()), lr));
        }
        if (couponId != null) {
            couponApi.use(memberId, couponId, order.getId());
        }
        if (OrderSubmitRequest.SOURCE_CART.equals(req.getSource())) {
            cartMapper.delete(Wrappers.<Cart>lambdaQuery()
                    .eq(Cart::getMemberId, memberId)
                    .in(Cart::getId, c.cartIds()));
        }
        String remark = c.seckill() == null ? "提交订单" : "提交秒杀订单（活动 " + c.seckill().id() + "）";
        stateMachine.log(order.getId(), null, OrderStatus.PENDING_PAY.name(), Operators.member(memberId), remark);
        return new OrderSubmitVO(order.getId(), order.getOrderNo(), order.getPayAmount(), order.getPayExpireAt());
    }

    /** 结算上下文：计价行（与 SKU 快照一一对应）、来源购物车项、秒杀活动、运费计算用的模板与重量 */
    record Checkout(List<Line> lines, Map<Long, SkuSnapshot> skus, List<Long> cartIds, SeckillInfo seckill,
                    Map<Long, ShippingInfo> shipping) {
    }

    /**
     * 运费：按收货省份走运费模板（同模板合并计件 / 计重，多模板相加，全局满额包邮优先），
     * 金额按会员折扣后的行金额判断包邮，与原「按折后金额包邮」口径一致。goodsLines 与 c.lines() 顺序一致。
     */
    private BigDecimal freightOf(Checkout c, List<GoodsLine> goodsLines, String province) {
        List<FreightCalculator.Item> items = new ArrayList<>(c.lines().size());
        for (int i = 0; i < c.lines().size(); i++) {
            Line l = c.lines().get(i);
            ShippingInfo s = c.shipping().get(l.skuId());
            items.add(new FreightCalculator.Item(s == null ? null : s.freightTemplateId(), l.quantity(),
                    s == null ? null : s.weight(), goodsLines.get(i).amount()));
        }
        return freightQuote.quote(province, items);
    }

    /** 解析下单商品并做通用校验：数量、在售、库存（库存最终以扣减时的条件更新为准） */
    Checkout prepare(Long memberId, OrderSubmitRequest req) {
        Map<Long, Integer> qtyBySku = new LinkedHashMap<>();
        List<Long> cartIds = List.of();
        if (OrderSubmitRequest.SOURCE_CART.equals(req.getSource())) {
            if (req.getSeckillId() != null) {
                throw new BusinessException("秒杀商品请通过立即购买下单");
            }
            if (CollectionUtils.isEmpty(req.getCartIds())) {
                throw new BusinessException("请选择要结算的商品");
            }
            Set<Long> ids = new LinkedHashSet<>(req.getCartIds());
            // 带 member_id 条件：只能结算自己的购物车
            List<Cart> carts = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                    .eq(Cart::getMemberId, memberId)
                    .in(Cart::getId, ids)
                    .orderByAsc(Cart::getCreatedAt));
            if (carts.size() != ids.size()) {
                throw new BusinessException("购物车商品已变化，请刷新后重试");
            }
            for (Cart cart : carts) {
                qtyBySku.merge(cart.getSkuId(), cart.getQuantity(), Integer::sum);
            }
            cartIds = new ArrayList<>(ids);
        } else {
            if (CollectionUtils.isEmpty(req.getItems())) {
                throw new BusinessException("请选择要购买的商品");
            }
            for (OrderSubmitRequest.Item it : req.getItems()) {
                if (it.getSkuId() == null || it.getQuantity() == null || it.getQuantity() < 1) {
                    throw new BusinessException("购买数量至少为 1");
                }
                qtyBySku.merge(it.getSkuId(), it.getQuantity(), Integer::sum);
            }
        }
        for (Integer q : qtyBySku.values()) {
            if (q > TradeDefaults.MAX_QUANTITY) {
                throw new BusinessException("单个商品最多购买 " + TradeDefaults.MAX_QUANTITY + " 件");
            }
        }

        SeckillInfo seckill = null;
        if (req.getSeckillId() != null) {
            seckill = validateSeckill(req.getSeckillId(), qtyBySku);
        }

        Map<Long, SkuSnapshot> skus = goodsApi.getSkus(qtyBySku.keySet());
        List<Line> lines = new ArrayList<>(qtyBySku.size());
        for (Map.Entry<Long, Integer> e : qtyBySku.entrySet()) {
            SkuSnapshot s = skus.get(e.getKey());
            if (s == null) {
                throw new BusinessException("商品不存在或已下架");
            }
            if (!s.onSale()) {
                throw new BusinessException(s.goodsName() + " 已下架");
            }
            if (s.stock() == null || s.stock() < e.getValue()) {
                throw new BusinessException(s.goodsName() + " 库存不足");
            }
            BigDecimal price = seckill != null ? seckill.seckillPrice() : s.price();
            lines.add(new Line(s.goodsId(), s.skuId(), s.categoryId(), price, e.getValue(), seckill != null));
        }
        Map<Long, ShippingInfo> shipping = goodsApi.getShipping(qtyBySku.keySet());
        return new Checkout(lines, skus, cartIds, seckill, shipping == null ? Map.of() : shipping);
    }

    /** 秒杀前置校验；名额（活动库存、个人限购）的最终判断在 SeckillApi.acquire 的 Lua 脚本里 */
    private SeckillInfo validateSeckill(Long seckillId, Map<Long, Integer> qtyBySku) {
        if (qtyBySku.size() != 1) {
            throw new BusinessException("秒杀订单每单只能购买一个商品");
        }
        SeckillInfo sk = seckillApi.get(seckillId);
        if (sk == null || !sk.enabled()) {
            throw new BusinessException("秒杀活动不存在或已下线");
        }
        LocalDateTime now = LocalDateTime.now();
        if (sk.startAt() != null && now.isBefore(sk.startAt())) {
            throw new BusinessException("秒杀活动尚未开始");
        }
        if (sk.endAt() != null && !now.isBefore(sk.endAt())) {
            throw new BusinessException("秒杀活动已结束");
        }
        Map.Entry<Long, Integer> only = qtyBySku.entrySet().iterator().next();
        if (!only.getKey().equals(sk.skuId())) {
            throw new BusinessException("秒杀商品与活动不匹配");
        }
        if (sk.limitPerMember() != null && sk.limitPerMember() > 0 && only.getValue() > sk.limitPerMember()) {
            throw new BusinessException("每人限购 " + sk.limitPerMember() + " 件");
        }
        return sk;
    }

    private Long bestCoupon(List<CouponOption> options) {
        CouponOption best = null;
        for (CouponOption o : options) {
            if (!o.usable() || o.discountAmount() == null || o.discountAmount().signum() <= 0) {
                continue;
            }
            if (best == null || o.discountAmount().compareTo(best.discountAmount()) > 0) {
                best = o;
            }
        }
        return best == null ? null : best.memberCouponId();
    }

    private MemberBrief requireMember(Long memberId) {
        MemberBrief m = memberApi.getMember(memberId);
        if (m == null) {
            throw new BusinessException("会员不存在");
        }
        if (m.status() != null && m.status() == 0) {
            throw new BusinessException("账号已停用，无法下单");
        }
        return m;
    }

    private AddressSnapshot requireAddress(Long memberId, Long addressId) {
        AddressSnapshot a = memberApi.getAddress(memberId, addressId);
        if (a == null) {
            throw new BusinessException("收货地址不存在");
        }
        return a;
    }

    private Order buildOrder(Long memberId, OrderSubmitRequest req, Checkout c, Result r, AddressSnapshot a,
                             Long couponId, LocalDateTime now) {
        Order o = new Order();
        // 先分配 ID：核销优惠券需要订单 ID，且订单号重试时 ID 保持不变
        o.setId(IdWorker.getId());
        o.setMemberId(memberId);
        o.setOrderType(c.seckill() == null ? OrderType.NORMAL.name() : OrderType.SECKILL.name());
        o.setSeckillId(c.seckill() == null ? null : c.seckill().id());
        o.setStatus(OrderStatus.PENDING_PAY.name());
        o.setGoodsAmount(r.goodsAmount());
        o.setFreightAmount(r.freightAmount());
        o.setCouponAmount(r.couponAmount());
        o.setDiscountAmount(r.discountAmount());
        o.setAdjustAmount(BigDecimal.ZERO.setScale(2));
        o.setPayAmount(r.payAmount());
        o.setMemberCouponId(couponId);
        o.setPayType("");
        o.setPayNo("");
        o.setPayExpireAt(now.plusMinutes(settings.payTimeoutMinutes()));
        o.setReceiverName(a.name());
        o.setReceiverMobile(a.mobile());
        o.setReceiverProvince(a.province());
        o.setReceiverCity(a.city());
        o.setReceiverDistrict(a.district());
        o.setReceiverDetail(a.detail());
        o.setRemark(StringUtils.hasText(req.getRemark()) ? req.getRemark().trim() : "");
        o.setAdminRemark("");
        o.setReviewed(0);
        o.setMemberDeleted(0);
        return o;
    }

    private OrderItem buildItem(Order order, SkuSnapshot s, LineResult lr) {
        OrderItem it = new OrderItem();
        it.setOrderId(order.getId());
        it.setMemberId(order.getMemberId());
        it.setGoodsId(s.goodsId());
        it.setSkuId(s.skuId());
        it.setGoodsName(s.goodsName());
        it.setPic(s.pic() == null ? "" : s.pic());
        it.setSpecText(s.specText() == null ? "" : s.specText());
        it.setPrice(lr.line().price());
        it.setQuantity(lr.line().quantity());
        it.setTotalAmount(lr.totalAmount());
        it.setPayAmount(lr.payAmount());
        it.setAftersaleStatus(OrderItem.AS_NONE);
        it.setReviewed(0);
        return it;
    }

    /** 唯一索引兜底：订单号冲突时换号重试（同一事务内 MySQL 只回滚失败语句，不影响已执行的扣库存） */
    private void insertWithUniqueNo(Order order) {
        for (int i = 1; ; i++) {
            order.setOrderNo(NoGenerator.orderNo());
            try {
                orderMapper.insert(order);
                return;
            } catch (DuplicateKeyException e) {
                if (i >= ORDER_NO_RETRY) {
                    log.error("订单号连续冲突 {} 次", i, e);
                    throw new BusinessException("下单人数过多，请稍后重试");
                }
            }
        }
    }

    private static OrderConfirmVO.AddressVO toAddressVO(Long id, AddressSnapshot a) {
        OrderConfirmVO.AddressVO vo = new OrderConfirmVO.AddressVO();
        vo.setId(id);
        vo.setName(a.name());
        vo.setMobile(a.mobile());
        vo.setProvince(a.province());
        vo.setCity(a.city());
        vo.setDistrict(a.district());
        vo.setDetail(a.detail());
        return vo;
    }
}
