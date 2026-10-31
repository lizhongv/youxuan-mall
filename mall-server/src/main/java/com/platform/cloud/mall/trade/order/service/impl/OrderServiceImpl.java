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
import com.platform.cloud.mall.trade.common.TradeDefaults;
import com.platform.cloud.mall.trade.common.TradeSettings;
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
        // 先锁订单行：售后申请同样先锁订单行，二者串行，避免「检查时无售后 → 买家刚申请成功 → 货已发出」
        Order o = orderMapper.selectByIdForUpdate(orderId);
        if (o == null) {
            throw new BusinessException("订单不存在");
        }
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
        List<OrderItem> items = itemsOf(o.getId());
        BigDecimal min = minPayAfterAdjust(o, items);
        if (newPay.compareTo(min) < 0) {
            // 下限 = 运费 + 每件商品 0.01：否则商品部分实付可能被压到 0，未发货退款时明细可退为 0、钱退不回去
            throw new BusinessException("改价后应付不能低于 " + min + " 元（运费 " + Money.scale(o.getFreightAmount())
                    + " 元 + 每件商品至少 0.01 元）");
        }
        // pay_amount 条件：与并发的改价 / 支付互斥，支付侧同样以读到的金额为条件落账
        int rows = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatus.PENDING_PAY.name())
                .eq(Order::getPayAmount, o.getPayAmount())
                .set(Order::getAdjustAmount, adjust)
                .set(Order::getPayAmount, newPay));
        if (rows == 0) {
            throw new BusinessException("订单状态或金额已变化，请刷新后重试");
        }
        reallocateItems(o, items, newPay);
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
    public List<Long> findExpiredUnpaid(Long afterId, int limit) {
        // 按 id 游标分页（不查总数），避免手拼 LIMIT：处理失败的订单不会一直占着队头，下一页从它之后继续
        return orderMapper.selectPage(new Page<>(1, Math.max(1, limit), false), Wrappers.<Order>lambdaQuery()
                        .select(Order::getId)
                        .eq(Order::getStatus, OrderStatus.PENDING_PAY.name())
                        .lt(Order::getPayExpireAt, LocalDateTime.now())
                        .gt(afterId != null, Order::getId, afterId)
                        .orderByAsc(Order::getId))
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
    public List<Long> findAutoReceivable(Long afterId, int limit) {
        return orderMapper.selectPage(new Page<>(1, Math.max(1, limit), false), Wrappers.<Order>lambdaQuery()
                        .select(Order::getId)
                        .eq(Order::getStatus, OrderStatus.DELIVERED.name())
                        .lt(Order::getAutoReceiveAt, LocalDateTime.now())
                        // 排除售后进行中的订单（会被 autoReceive 跳过）：否则它们按时间排在队头，
                        // 积压到一批的数量后，后面真正可收货的订单永远轮不到。常量 SQL，不含入参
                        .notExists(NO_APPLYING_AFTERSALE)
                        .gt(afterId != null, Order::getId, afterId)
                        .orderByAsc(Order::getId))
                .getRecords().stream().map(Order::getId).toList();
    }

    /** 订单没有售后进行中的明细；引用外层 trd_order.id，不拼接任何入参 */
    static final String NO_APPLYING_AFTERSALE = "SELECT 1 FROM trd_order_item i WHERE i.order_id = trd_order.id "
            + "AND i.deleted = 0 AND i.aftersale_status = '" + OrderItem.AS_APPLYING + "'";

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
     * 秒杀名额在本事务内直接 release：数据库 sold 随本事务提交 / 回滚，Redis 名额由 release 挂在本事务的
     * afterCommit 上恰好加回一次。不能把 release 整个放进 afterCommit——那时事务已结束，
     * release 内部再登记的 afterCommit 不会触发，Redis 名额就永远不还了。
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
        releaseSeckill(o, items, seckillApi);
    }

    /** 归还秒杀名额（取消 / 关单 / 未发货整单退款共用）；须在业务事务内调用 */
    public static void releaseSeckill(Order o, List<OrderItem> items, SeckillApi seckillApi) {
        if (!OrderType.SECKILL.name().equals(o.getOrderType())) {
            return;
        }
        if (o.getSeckillId() == null) {
            log.error("秒杀订单缺少活动 ID，名额需人工归还 orderId={}", o.getId());
            return;
        }
        int qty = items.stream().mapToInt(OrderItem::getQuantity).sum();
        seckillApi.release(o.getSeckillId(), o.getMemberId(), qty);
    }

    /**
     * 确认收货：状态 → COMPLETED，按「实付 − 已退款」发放积分与成长值。
     * 同时把发放数量与计算基数记在订单上，收货后再退款时按「退款额 / 基数」比例回收，不受运营后来改配置影响。
     */
    void receive(Order o, String operator, String remark) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal refunded = Money.nz(aftersaleMapper.sumRefundedByOrder(o.getId()));
        BigDecimal base = Money.scale(Money.nz(o.getPayAmount()).subtract(refunded).max(BigDecimal.ZERO));
        int points = rewardOf(base, settings.pointsPerYuan());
        int growth = rewardOf(base, settings.growthPerYuan());
        stateMachine.transit(o, OrderStatus.COMPLETED, operator, remark, w -> w.set(Order::getReceiveTime, now)
                .set(Order::getRewardPoints, points)
                .set(Order::getRewardGrowth, growth)
                .set(Order::getRewardBase, base));
        o.setReceiveTime(now);
        o.setRewardPoints(points);
        o.setRewardGrowth(growth);
        o.setRewardBase(base);
        if (points > 0) {
            memberApi.changePoints(o.getMemberId(), points, BIZ_REWARD, o.getOrderNo(), "订单完成赠送积分");
        }
        if (growth > 0) {
            memberApi.addGrowth(o.getMemberId(), growth, BIZ_REWARD, o.getOrderNo(), "订单完成增加成长值");
        }
    }

    /** 实付 × 每元奖励，向下取整：不足 1 分的部分不奖励，避免积分超发 */
    public static int rewardOf(BigDecimal amount, BigDecimal perYuan) {
        if (amount == null || perYuan == null || amount.signum() <= 0 || perYuan.signum() <= 0) {
            return 0;
        }
        return amount.multiply(perYuan).setScale(0, RoundingMode.DOWN).intValueExact();
    }

    /**
     * 改价下限 = 运费 + 0.01 × 商品件数（且不低于 0.01）。
     * 取舍：按件数而不是按明细行数，保证每件商品分摊后至少 0.01 元，部分数量退款时也有钱可退。
     */
    static BigDecimal minPayAfterAdjust(Order o, List<OrderItem> items) {
        int qty = items.stream().mapToInt(i -> i.getQuantity() == null ? 0 : i.getQuantity()).sum();
        BigDecimal min = Money.nz(o.getFreightAmount()).add(TradeDefaults.MIN_PAY_AMOUNT.multiply(BigDecimal.valueOf(qty)));
        return Money.scale(min.max(TradeDefaults.MIN_PAY_AMOUNT));
    }

    /**
     * 改价后把商品部分实付按原比例重新分摊到明细，保证售后退款上限与实付一致。
     * 先给每行预留 0.01 × 件数，再按比例分摊剩余部分，避免比例极小的行被四舍五入成 0。
     */
    private void reallocateItems(Order o, List<OrderItem> items, BigDecimal newPay) {
        if (items.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, BigDecimal> e : allocateItems(o, items, newPay).entrySet()) {
            orderItemMapper.update(null, Wrappers.<OrderItem>lambdaUpdate()
                    .eq(OrderItem::getId, e.getKey())
                    .set(OrderItem::getPayAmount, e.getValue()));
        }
    }

    /** 纯计算：明细 ID → 新的实付，便于单测 */
    static Map<Long, BigDecimal> allocateItems(Order o, List<OrderItem> items, BigDecimal newPay) {
        BigDecimal itemsTotal = newPay.subtract(Money.nz(o.getFreightAmount())).max(BigDecimal.ZERO);
        List<BigDecimal> reserves = new ArrayList<>(items.size());
        BigDecimal reserved = BigDecimal.ZERO;
        for (OrderItem i : items) {
            BigDecimal r = TradeDefaults.MIN_PAY_AMOUNT.multiply(BigDecimal.valueOf(i.getQuantity() == null ? 0 : i.getQuantity()));
            reserves.add(r);
            reserved = reserved.add(r);
        }
        BigDecimal rest = itemsTotal.subtract(reserved);
        if (rest.signum() < 0) {
            // 调用方已校验下限，正常不会出现；兜底退回到纯比例分摊
            rest = itemsTotal;
            reserves.replaceAll(r -> BigDecimal.ZERO);
        }
        List<BigDecimal> weights = new ArrayList<>(items.size());
        boolean allZero = items.stream().allMatch(i -> Money.nz(i.getPayAmount()).signum() == 0);
        for (OrderItem i : items) {
            weights.add(allZero ? Money.nz(i.getTotalAmount()) : Money.nz(i.getPayAmount()));
        }
        List<BigDecimal> shares = PriceCalculator.allocate(rest, weights, false);
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (int k = 0; k < items.size(); k++) {
            result.put(items.get(k).getId(), Money.scale(shares.get(k).add(reserves.get(k))));
        }
        return result;
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
