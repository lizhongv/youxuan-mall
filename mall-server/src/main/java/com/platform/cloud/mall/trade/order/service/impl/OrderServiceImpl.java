package com.platform.cloud.mall.trade.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.marketing.api.CouponApi;
import com.platform.cloud.mall.marketing.api.SeckillApi;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.member.api.MemberApi.MemberBrief;
import com.platform.cloud.mall.trade.aftersale.mapper.AftersaleMapper;
import com.platform.cloud.mall.trade.common.Money;
import com.platform.cloud.mall.trade.common.Operators;
import com.platform.cloud.mall.trade.common.SeckillMarker;
import com.platform.cloud.mall.trade.common.TradeDefaults;
import com.platform.cloud.mall.trade.common.TradeSettings;
import com.platform.cloud.mall.trade.common.TxHooks;
import com.platform.cloud.mall.trade.order.dto.AdminOrderPageQuery;
import com.platform.cloud.mall.trade.order.dto.OrderDeliverRequest;
import com.platform.cloud.mall.trade.order.dto.OrderPageQuery;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.entity.OrderItem;
import com.platform.cloud.mall.trade.order.entity.OrderLog;
import com.platform.cloud.mall.trade.order.enums.OrderStatus;
import com.platform.cloud.mall.trade.order.enums.OrderType;
import com.platform.cloud.mall.trade.order.mapper.OrderItemMapper;
import com.platform.cloud.mall.trade.order.mapper.OrderLogMapper;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.order.mapper.row.StatusCountRow;
import com.platform.cloud.mall.trade.order.service.OrderAssembler;
import com.platform.cloud.mall.trade.order.service.OrderService;
import com.platform.cloud.mall.trade.order.service.OrderStateMachine;
import com.platform.cloud.mall.trade.order.service.PriceCalculator;
import com.platform.cloud.mall.trade.order.vo.OrderLogVO;
import com.platform.cloud.mall.trade.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单查询与状态操作。所有状态变更经 {@link OrderStateMachine#transit}，保证条件更新 + 日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** mbr_account_log.biz_type：确认收货奖励 */
    static final String BIZ_REWARD = "ORDER_REWARD";

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderLogMapper orderLogMapper;
    private final AftersaleMapper aftersaleMapper;
    private final OrderStateMachine stateMachine;
    private final GoodsApi goodsApi;
    private final CouponApi couponApi;
    private final SeckillApi seckillApi;
    private final MemberApi memberApi;
    private final TradeSettings settings;

    // ------------------------------------------------------------------ 会员

    @Override
    public PageVO<OrderVO> memberPage(Long memberId, OrderPageQuery q) {
        LambdaQueryWrapper<Order> w = Wrappers.<Order>lambdaQuery()
                .eq(Order::getMemberId, memberId)
                .eq(Order::getMemberDeleted, 0);
        if (OrderPageQuery.TO_REVIEW.equals(q.getStatus())) {
            w.eq(Order::getStatus, OrderStatus.COMPLETED.name()).eq(Order::getReviewed, 0);
        } else if (StringUtils.hasText(q.getStatus())) {
            w.eq(Order::getStatus, q.getStatus());
        }
        w.orderByDesc(Order::getCreatedAt).orderByDesc(Order::getId);
        Page<Order> page = orderMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w);
        Map<Long, List<OrderItem>> items = itemsOf(page.getRecords());
        return PageVO.of(page, o -> OrderAssembler.toVO(o, items.get(o.getId())));
    }

    @Override
    public OrderVO memberDetail(Long memberId, Long orderId) {
        Order o = requireMemberOrder(memberId, orderId);
        OrderVO vo = OrderAssembler.toVO(o, itemsOf(o.getId()));
        vo.setLogs(logsOf(o.getId()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberCancel(Long memberId, Long orderId, String reason) {
        Order o = requireMemberOrder(memberId, orderId);
        requireStatus(o, OrderStatus.PENDING_PAY, "仅待付款订单可取消");
        cancel(o, Operators.member(memberId), StringUtils.hasText(reason) ? reason : "买家取消");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberReceive(Long memberId, Long orderId) {
        Order o = requireMemberOrder(memberId, orderId);
        requireStatus(o, OrderStatus.DELIVERED, "仅待收货订单可确认收货");
        receive(o, Operators.member(memberId), "买家确认收货");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberDelete(Long memberId, Long orderId) {
        Order o = requireMemberOrder(memberId, orderId);
        OrderStatus s = OrderStatus.of(o.getStatus());
        if (s != OrderStatus.COMPLETED && s != OrderStatus.CANCELLED) {
            throw new BusinessException("仅已完成或已取消的订单可删除");
        }
        // 只对买家隐藏，后台与统计仍保留，便于售后与对账
        orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(Order::getMemberId, memberId)
                .set(Order::getMemberDeleted, 1));
    }

    // ------------------------------------------------------------------ 管理端

    @Override
    public PageVO<OrderVO> adminPage(AdminOrderPageQuery q) {
        LambdaQueryWrapper<Order> w = Wrappers.<Order>lambdaQuery()
                .eq(StringUtils.hasText(q.getOrderNo()), Order::getOrderNo, trim(q.getOrderNo()))
                .eq(StringUtils.hasText(q.getReceiverMobile()), Order::getReceiverMobile, trim(q.getReceiverMobile()))
                .eq(StringUtils.hasText(q.getStatus()), Order::getStatus, q.getStatus())
                .eq(StringUtils.hasText(q.getOrderType()), Order::getOrderType, q.getOrderType())
                .ge(q.getStartTime() != null, Order::getCreatedAt, q.getStartTime())
                .le(q.getEndTime() != null, Order::getCreatedAt, q.getEndTime())
                .orderByDesc(Order::getCreatedAt).orderByDesc(Order::getId);
        Page<Order> page = orderMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w);
        Map<Long, List<OrderItem>> items = itemsOf(page.getRecords());
        Set<Long> memberIds = page.getRecords().stream().map(Order::getMemberId).collect(Collectors.toSet());
        Map<Long, MemberBrief> members = memberIds.isEmpty() ? Map.of() : memberApi.getMembers(memberIds);
        return PageVO.of(page, o -> {
            OrderVO vo = OrderAssembler.toVO(o, items.get(o.getId()));
            MemberBrief m = members.get(o.getMemberId());
            vo.setMemberNickname(m == null ? "" : m.nickname());
            return vo;
        });
    }

    @Override
    public OrderVO adminDetail(Long orderId) {
        Order o = requireOrder(orderId);
        OrderVO vo = OrderAssembler.toVO(o, itemsOf(o.getId()));
        vo.setLogs(logsOf(o.getId()));
        MemberBrief m = memberApi.getMember(o.getMemberId());
        OrderVO.MemberInfo info = new OrderVO.MemberInfo();
        info.setId(o.getMemberId());
        if (m != null) {
            info.setNickname(m.nickname());
            info.setMobile(m.mobile());
            vo.setMemberNickname(m.nickname());
        }
        vo.setMember(info);
        return vo;
    }

    @Override
    public Map<String, Long> statusCount() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            result.put(s.name(), 0L);
        }
        for (StatusCountRow r : orderMapper.countByStatus()) {
            if (result.containsKey(r.getStatus())) {
                result.put(r.getStatus(), r.getCnt());
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliver(Long adminId, Long orderId, OrderDeliverRequest req) {
        Order o = requireOrder(orderId);
        requireStatus(o, OrderStatus.PENDING_DELIVERY, "仅待发货订单可发货");
        // 有退款申请未处理就发货，容易出现「钱退了货也发了」，先处理售后
        long applying = orderItemMapper.selectCount(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .eq(OrderItem::getAftersaleStatus, OrderItem.AS_APPLYING));
        if (applying > 0) {
            throw new BusinessException("订单有处理中的售后，请先处理后再发货");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime autoReceiveAt = now.plusDays(settings.autoReceiveDays());
        String company = req.getDeliveryCompany().trim();
        String no = req.getDeliveryNo().trim();
        stateMachine.transit(o, OrderStatus.DELIVERED, Operators.admin(adminId), "发货：" + company + " " + no,
                w -> w.set(Order::getDeliveryCompany, company)
                        .set(Order::getDeliveryNo, no)
                        .set(Order::getDeliveryTime, now)
                        .set(Order::getAutoReceiveAt, autoReceiveAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustPrice(Long adminId, Long orderId, BigDecimal adjustAmount) {
        Order o = requireOrder(orderId);
        requireStatus(o, OrderStatus.PENDING_PAY, "仅待付款订单可改价");
        BigDecimal adjust = Money.scale(adjustAmount);
        // adjustAmount 是相对原始应付的总调整额：先还原上次改价，再叠加本次
        BigDecimal base = Money.nz(o.getPayAmount()).subtract(Money.nz(o.getAdjustAmount()));
        BigDecimal newPay = Money.scale(base.add(adjust));
        if (newPay.compareTo(TradeDefaults.MIN_PAY_AMOUNT) < 0) {
            throw new BusinessException("改价后实付金额不能低于 0.01");
        }
        int rows = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatus.PENDING_PAY.name())
                .set(Order::getAdjustAmount, adjust)
                .set(Order::getPayAmount, newPay));
        if (rows == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        reallocateItems(o, newPay);
        stateMachine.log(orderId, o.getStatus(), o.getStatus(), Operators.admin(adminId),
                "改价：调整 " + adjust + "，应付 " + o.getPayAmount() + " → " + newPay);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAdminRemark(Long adminId, Long orderId, String adminRemark) {
        requireOrder(orderId);
        orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, orderId)
                .set(Order::getAdminRemark, adminRemark == null ? "" : adminRemark.trim()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminCancel(Long adminId, Long orderId, String reason) {
        Order o = requireOrder(orderId);
        requireStatus(o, OrderStatus.PENDING_PAY, "仅待付款订单可取消");
        cancel(o, Operators.admin(adminId), StringUtils.hasText(reason) ? reason : "商家取消");
    }

    // ------------------------------------------------------------------ 定时任务

    @Override
    public List<Long> findExpiredUnpaid(int limit) {
        // 用分页取前 N 条（不查总数），避免手拼 LIMIT
        return orderMapper.selectPage(new Page<>(1, Math.max(1, limit), false), Wrappers.<Order>lambdaQuery()
                        .select(Order::getId)
                        .eq(Order::getStatus, OrderStatus.PENDING_PAY.name())
                        .lt(Order::getPayExpireAt, LocalDateTime.now())
                        .orderByAsc(Order::getPayExpireAt))
                .getRecords().stream().map(Order::getId).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeExpired(Long orderId) {
        Order o = orderMapper.selectById(orderId);
        // 重新读取后再判断：查询到执行之间买家可能刚好完成了支付
        if (o == null || !OrderStatus.PENDING_PAY.name().equals(o.getStatus())
                || o.getPayExpireAt() == null || o.getPayExpireAt().isAfter(LocalDateTime.now())) {
            return;
        }
        cancel(o, Operators.SYSTEM, "超时未支付，系统自动关闭");
    }

    @Override
    public List<Long> findAutoReceivable(int limit) {
        return orderMapper.selectPage(new Page<>(1, Math.max(1, limit), false), Wrappers.<Order>lambdaQuery()
                        .select(Order::getId)
                        .eq(Order::getStatus, OrderStatus.DELIVERED.name())
                        .lt(Order::getAutoReceiveAt, LocalDateTime.now())
                        .orderByAsc(Order::getAutoReceiveAt))
                .getRecords().stream().map(Order::getId).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoReceive(Long orderId) {
        Order o = orderMapper.selectById(orderId);
        if (o == null || !OrderStatus.DELIVERED.name().equals(o.getStatus())
                || o.getAutoReceiveAt() == null || o.getAutoReceiveAt().isAfter(LocalDateTime.now())) {
            return;
        }
        // 售后处理中暂停自动收货，避免买家维权期间订单被系统完结
        long applying = orderItemMapper.selectCount(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .eq(OrderItem::getAftersaleStatus, OrderItem.AS_APPLYING));
        if (applying > 0) {
            return;
        }
        receive(o, Operators.SYSTEM, "超时自动确认收货");
    }

    // ------------------------------------------------------------------ 内部

    /**
     * 取消 / 关单：状态 → CANCELLED，回补库存、退还优惠券、归还秒杀名额。
     * 秒杀名额在 Redis，放在事务提交后归还，避免本事务回滚却已放出名额。
     */
    void cancel(Order o, String operator, String reason) {
        LocalDateTime now = LocalDateTime.now();
        String r = reason.length() > 255 ? reason.substring(0, 255) : reason;
        stateMachine.transit(o, OrderStatus.CANCELLED, operator, r,
                w -> w.set(Order::getCancelTime, now).set(Order::getCancelReason, r));
        List<OrderItem> items = itemsOf(o.getId());
        for (OrderItem it : items) {
            goodsApi.restoreStock(it.getSkuId(), it.getQuantity());
        }
        if (o.getMemberCouponId() != null) {
            couponApi.giveBack(o.getMemberCouponId());
        }
        if (OrderType.SECKILL.name().equals(o.getOrderType())) {
            Long seckillId = findSeckillId(o.getId());
            if (seckillId == null) {
                log.error("秒杀订单未找到活动 ID，名额需人工归还 orderId={}", o.getId());
            } else {
                int qty = items.stream().mapToInt(OrderItem::getQuantity).sum();
                Long memberId = o.getMemberId();
                TxHooks.afterCommit(() -> seckillApi.release(seckillId, memberId, qty));
            }
        }
    }

    /** 确认收货：状态 → COMPLETED，按「实付 − 已退款」发放积分与成长值 */
    void receive(Order o, String operator, String remark) {
        LocalDateTime now = LocalDateTime.now();
        stateMachine.transit(o, OrderStatus.COMPLETED, operator, remark, w -> w.set(Order::getReceiveTime, now));
        o.setReceiveTime(now);
        BigDecimal refunded = Money.nz(aftersaleMapper.sumRefundedByOrder(o.getId()));
        BigDecimal base = Money.nz(o.getPayAmount()).subtract(refunded).max(BigDecimal.ZERO);
        int points = rewardOf(base, settings.pointsPerYuan());
        int growth = rewardOf(base, settings.growthPerYuan());
        if (points > 0) {
            memberApi.changePoints(o.getMemberId(), points, BIZ_REWARD, o.getOrderNo(), "订单完成赠送积分");
        }
        if (growth > 0) {
            memberApi.addGrowth(o.getMemberId(), growth, BIZ_REWARD, o.getOrderNo(), "订单完成增加成长值");
        }
    }

    /** 实付 × 每元奖励，向下取整：不足 1 分的部分不奖励，避免积分超发 */
    static int rewardOf(BigDecimal amount, BigDecimal perYuan) {
        if (amount == null || perYuan == null || amount.signum() <= 0 || perYuan.signum() <= 0) {
            return 0;
        }
        return amount.multiply(perYuan).setScale(0, RoundingMode.DOWN).intValueExact();
    }

    /** 改价后把商品部分实付按原比例重新分摊到明细，保证售后退款上限与实付一致 */
    private void reallocateItems(Order o, BigDecimal newPay) {
        List<OrderItem> items = itemsOf(o.getId());
        if (items.isEmpty()) {
            return;
        }
        BigDecimal itemsTotal = newPay.subtract(Money.nz(o.getFreightAmount())).max(BigDecimal.ZERO);
        List<BigDecimal> weights = new ArrayList<>(items.size());
        boolean allZero = items.stream().allMatch(i -> Money.nz(i.getPayAmount()).signum() == 0);
        for (OrderItem i : items) {
            weights.add(allZero ? Money.nz(i.getTotalAmount()) : Money.nz(i.getPayAmount()));
        }
        List<BigDecimal> shares = PriceCalculator.allocate(itemsTotal, weights, false);
        for (int k = 0; k < items.size(); k++) {
            orderItemMapper.update(null, Wrappers.<OrderItem>lambdaUpdate()
                    .eq(OrderItem::getId, items.get(k).getId())
                    .set(OrderItem::getPayAmount, shares.get(k)));
        }
    }

    private Long findSeckillId(Long orderId) {
        List<OrderLog> logs = orderLogMapper.selectList(Wrappers.<OrderLog>lambdaQuery()
                .eq(OrderLog::getOrderId, orderId)
                .eq(OrderLog::getFromStatus, "")
                .orderByAsc(OrderLog::getId));
        for (OrderLog l : logs) {
            Long id = SeckillMarker.parse(l.getRemark());
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    private Order requireMemberOrder(Long memberId, Long orderId) {
        // 带 member_id 条件查询：他人订单与不存在一样返回「订单不存在」，不泄露订单是否存在
        Order o = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getId, orderId)
                .eq(Order::getMemberId, memberId)
                .eq(Order::getMemberDeleted, 0));
        if (o == null) {
            throw new BusinessException("订单不存在");
        }
        return o;
    }

    private Order requireOrder(Long orderId) {
        Order o = orderMapper.selectById(orderId);
        if (o == null) {
            throw new BusinessException("订单不存在");
        }
        return o;
    }

    private static void requireStatus(Order o, OrderStatus expected, String msg) {
        if (!expected.name().equals(o.getStatus())) {
            throw new BusinessException(msg);
        }
    }

    private List<OrderItem> itemsOf(Long orderId) {
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId));
    }

    private Map<Long, List<OrderItem>> itemsOf(List<Order> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = orders.stream().map(Order::getId).toList();
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .in(OrderItem::getOrderId, ids)
                        .orderByAsc(OrderItem::getId))
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
    }

    private List<OrderLogVO> logsOf(Long orderId) {
        return orderLogMapper.selectList(Wrappers.<OrderLog>lambdaQuery()
                        .eq(OrderLog::getOrderId, orderId)
                        .orderByAsc(OrderLog::getCreatedAt).orderByAsc(OrderLog::getId))
                .stream().map(OrderAssembler::toLogVO).toList();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
