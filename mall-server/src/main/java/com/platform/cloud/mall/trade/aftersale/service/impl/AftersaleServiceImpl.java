package com.platform.cloud.mall.trade.aftersale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.member.api.MemberApi.MemberBrief;
import com.platform.cloud.mall.trade.aftersale.dto.AftersaleApplyRequest;
import com.platform.cloud.mall.trade.aftersale.dto.AftersalePageQuery;
import com.platform.cloud.mall.trade.aftersale.dto.AftersaleReturnRequest;
import com.platform.cloud.mall.trade.aftersale.entity.Aftersale;
import com.platform.cloud.mall.trade.aftersale.enums.AftersaleStatus;
import com.platform.cloud.mall.trade.aftersale.enums.AftersaleType;
import com.platform.cloud.mall.trade.aftersale.mapper.AftersaleMapper;
import com.platform.cloud.mall.trade.aftersale.service.AftersaleService;
import com.platform.cloud.mall.trade.aftersale.vo.AftersaleVO;
import com.platform.cloud.mall.trade.common.Money;
import com.platform.cloud.mall.trade.common.NoGenerator;
import com.platform.cloud.mall.trade.common.Operators;
import com.platform.cloud.mall.trade.common.TradeSettings;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.entity.OrderItem;
import com.platform.cloud.mall.trade.order.enums.OrderStatus;
import com.platform.cloud.mall.trade.order.mapper.OrderItemMapper;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.order.service.OrderAssembler;
import com.platform.cloud.mall.trade.order.service.OrderStateMachine;
import com.platform.cloud.mall.trade.pay.service.PayService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 售后服务。
 * <p>
 * 关键约束：
 * <ul>
 *   <li>退款额度：申请金额 ≤ 明细 payAmount − 已退款金额，退款执行时再复核一次，防止并发超退。</li>
 *   <li>同一明细同时只能有一个进行中的售后：用明细 aftersale_status 的条件更新（≠ APPLYING 才能改成 APPLYING）做互斥。</li>
 *   <li>售后状态变更全部是 {@code WHERE status = <原状态>} 的条件更新，重复点击「同意」不会退两次款。</li>
 *   <li>未发货订单仅退款时须全额退该商品，退款后回补库存；全部明细退完则订单取消（不退券）。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AftersaleServiceImpl implements AftersaleService {

    private static final int NO_RETRY = 3;

    private final AftersaleMapper aftersaleMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStateMachine orderStateMachine;
    private final PayService payService;
    private final GoodsApi goodsApi;
    private final MemberApi memberApi;
    private final TradeSettings settings;

    // ------------------------------------------------------------------ 会员

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long memberId, AftersaleApplyRequest req) {
        OrderItem item = orderItemMapper.selectOne(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getId, req.getOrderItemId())
                .eq(OrderItem::getMemberId, memberId));
        if (item == null) {
            throw new BusinessException("订单商品不存在");
        }
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getId, item.getOrderId())
                .eq(Order::getMemberId, memberId));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        AftersaleType type = AftersaleType.valueOf(req.getType());
        OrderStatus orderStatus = OrderStatus.of(order.getStatus());
        switch (orderStatus) {
            case PENDING_DELIVERY -> {
                if (type != AftersaleType.REFUND) {
                    throw new BusinessException("未发货订单仅支持仅退款");
                }
            }
            case DELIVERED -> {
                // 待收货：可仅退款（如未收到货）或退货退款
            }
            case COMPLETED -> {
                LocalDateTime deadline = order.getReceiveTime() == null ? null
                        : order.getReceiveTime().plusDays(settings.aftersaleDays());
                if (deadline == null || LocalDateTime.now().isAfter(deadline)) {
                    throw new BusinessException("已超过售后期，无法申请");
                }
            }
            default -> throw new BusinessException("订单" + orderStatus.text() + "，不能申请售后");
        }

        BigDecimal remaining = remainingRefundable(item);
        if (remaining.signum() <= 0) {
            throw new BusinessException("该商品已全部退款");
        }
        BigDecimal amount = Money.scale(req.getAmount());
        if (amount.compareTo(remaining) > 0) {
            throw new BusinessException("退款金额不能超过 " + remaining + " 元");
        }
        if (orderStatus == OrderStatus.PENDING_DELIVERY && amount.compareTo(remaining) != 0) {
            // 未发货的商品退款后不再发货并回补库存，部分退款会导致货与钱对不上
            throw new BusinessException("未发货商品需全额退款，可退金额 " + remaining + " 元");
        }

        // 互斥占位：明细不在售后中才能发起，并发的第二个申请在这里失败
        int rows = orderItemMapper.update(null, Wrappers.<OrderItem>lambdaUpdate()
                .eq(OrderItem::getId, item.getId())
                .ne(OrderItem::getAftersaleStatus, OrderItem.AS_APPLYING)
                .set(OrderItem::getAftersaleStatus, OrderItem.AS_APPLYING));
        if (rows == 0) {
            throw new BusinessException("该商品已有进行中的售后");
        }

        Aftersale a = new Aftersale();
        a.setOrderId(order.getId());
        a.setOrderItemId(item.getId());
        a.setMemberId(memberId);
        a.setType(type.name());
        a.setStatus(AftersaleStatus.APPLYING.name());
        a.setReason(req.getReason().trim());
        a.setDescription(req.getDescription() == null ? "" : req.getDescription().trim());
        a.setImages(req.getImages() == null ? List.of() : req.getImages());
        a.setAmount(amount);
        a.setQuantity(item.getQuantity());
        a.setAuditRemark("");
        a.setReturnCompany("");
        a.setReturnNo("");
        insertWithUniqueNo(a);
        return a.getId();
    }

    @Override
    public PageVO<AftersaleVO> memberPage(Long memberId, AftersalePageQuery q) {
        LambdaQueryWrapper<Aftersale> w = Wrappers.<Aftersale>lambdaQuery()
                .eq(Aftersale::getMemberId, memberId)
                .eq(StringUtils.hasText(q.getStatus()), Aftersale::getStatus, q.getStatus())
                .orderByDesc(Aftersale::getCreatedAt).orderByDesc(Aftersale::getId);
        return toPage(aftersaleMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w), false);
    }

    @Override
    public AftersaleVO memberDetail(Long memberId, Long id) {
        return toDetail(requireMemberAftersale(memberId, id), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberCancel(Long memberId, Long id) {
        Aftersale a = requireMemberAftersale(memberId, id);
        AftersaleStatus s = AftersaleStatus.of(a.getStatus());
        if (s != AftersaleStatus.APPLYING && s != AftersaleStatus.WAIT_RETURN) {
            throw new BusinessException("售后单" + s.text() + "，不能撤销");
        }
        transit(a, AftersaleStatus.CANCELLED, null);
        releaseItem(a.getOrderItemId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberReturn(Long memberId, Long id, AftersaleReturnRequest req) {
        Aftersale a = requireMemberAftersale(memberId, id);
        String company = req.getReturnCompany().trim();
        String no = req.getReturnNo().trim();
        transit(a, AftersaleStatus.RETURNING, w -> w.set(Aftersale::getReturnCompany, company)
                .set(Aftersale::getReturnNo, no));
    }

    // ------------------------------------------------------------------ 管理端

    @Override
    public PageVO<AftersaleVO> adminPage(AftersalePageQuery q) {
        LambdaQueryWrapper<Aftersale> w = Wrappers.<Aftersale>lambdaQuery()
                .eq(StringUtils.hasText(q.getAftersaleNo()), Aftersale::getAftersaleNo, trim(q.getAftersaleNo()))
                .eq(StringUtils.hasText(q.getStatus()), Aftersale::getStatus, q.getStatus())
                .eq(StringUtils.hasText(q.getType()), Aftersale::getType, q.getType());
        if (StringUtils.hasText(q.getOrderNo())) {
            Order o = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                    .select(Order::getId).eq(Order::getOrderNo, q.getOrderNo().trim()));
            if (o == null) {
                return toPage(new Page<>(q.getPage(), q.getSize()), true);
            }
            w.eq(Aftersale::getOrderId, o.getId());
        }
        w.orderByDesc(Aftersale::getCreatedAt).orderByDesc(Aftersale::getId);
        return toPage(aftersaleMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w), true);
    }

    @Override
    public AftersaleVO adminDetail(Long id) {
        return toDetail(requireAftersale(id), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long adminId, Long id, String remark) {
        Aftersale a = requireAftersale(id);
        if (!AftersaleStatus.APPLYING.name().equals(a.getStatus())) {
            throw new BusinessException("仅申请中的售后可审核");
        }
        LocalDateTime now = LocalDateTime.now();
        String r = remark == null ? "" : remark.trim();
        Consumer<LambdaUpdateWrapper<Aftersale>> audit = w -> w.set(Aftersale::getAuditRemark, r)
                .set(Aftersale::getAuditTime, now);
        if (AftersaleType.REFUND.name().equals(a.getType())) {
            refund(a, Operators.admin(adminId), audit);
        } else {
            transit(a, AftersaleStatus.WAIT_RETURN, audit);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long adminId, Long id, String remark) {
        Aftersale a = requireAftersale(id);
        if (!AftersaleStatus.APPLYING.name().equals(a.getStatus())) {
            throw new BusinessException("仅申请中的售后可拒绝");
        }
        String r = remark == null ? "" : remark.trim();
        LocalDateTime now = LocalDateTime.now();
        transit(a, AftersaleStatus.REJECTED, w -> w.set(Aftersale::getAuditRemark, r)
                .set(Aftersale::getAuditTime, now));
        releaseItem(a.getOrderItemId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveReturn(Long adminId, Long id) {
        Aftersale a = requireAftersale(id);
        if (!AftersaleStatus.RETURNING.name().equals(a.getStatus())) {
            throw new BusinessException("仅退货中的售后可确认收货");
        }
        refund(a, Operators.admin(adminId), null);
    }

    // ------------------------------------------------------------------ 内部

    /**
     * 执行退款：复核额度 → 原路退款 → 售后单 REFUNDED → 明细 REFUNDED；
     * 未发货订单回补库存，且全部明细退完时订单取消（优惠券不退，已按实付分摊退给买家）。
     */
    void refund(Aftersale a, String operator, Consumer<LambdaUpdateWrapper<Aftersale>> extra) {
        Order order = orderMapper.selectById(a.getOrderId());
        OrderItem item = orderItemMapper.selectById(a.getOrderItemId());
        if (order == null || item == null) {
            throw new BusinessException("订单不存在");
        }
        if (a.getAmount().compareTo(remainingRefundable(item)) > 0) {
            throw new BusinessException("退款金额超过可退金额，请拒绝后让买家重新申请");
        }
        LocalDateTime now = LocalDateTime.now();
        // 先改状态再调渠道：条件更新失败（重复审核）时不会多退一次款
        transit(a, AftersaleStatus.REFUNDED, w -> {
            w.set(Aftersale::getRefundTime, now);
            if (extra != null) {
                extra.accept(w);
            }
        });
        payService.refund(order, a.getAmount(), a.getAftersaleNo());
        orderItemMapper.update(null, Wrappers.<OrderItem>lambdaUpdate()
                .eq(OrderItem::getId, item.getId())
                .set(OrderItem::getAftersaleStatus, OrderItem.AS_REFUNDED));

        if (OrderStatus.PENDING_DELIVERY.name().equals(order.getStatus())) {
            goodsApi.restoreStock(item.getSkuId(), item.getQuantity());
            long notRefunded = orderItemMapper.selectCount(Wrappers.<OrderItem>lambdaQuery()
                    .eq(OrderItem::getOrderId, order.getId())
                    .ne(OrderItem::getAftersaleStatus, OrderItem.AS_REFUNDED));
            if (notRefunded == 0) {
                String reason = "全部商品已退款";
                orderStateMachine.transit(order, OrderStatus.CANCELLED, operator, reason,
                        w -> w.set(Order::getCancelTime, now).set(Order::getCancelReason, reason));
                payService.markRefunded(order.getId());
            }
        }
    }

    /** 售后状态的条件更新：从读取时的状态改到目标状态，影响 0 行说明已被并发处理 */
    void transit(Aftersale a, AftersaleStatus to, Consumer<LambdaUpdateWrapper<Aftersale>> extra) {
        AftersaleStatus from = AftersaleStatus.of(a.getStatus());
        if (!from.canTransitTo(to)) {
            throw new BusinessException("售后单" + from.text() + "，不能变更为" + to.text());
        }
        LambdaUpdateWrapper<Aftersale> w = Wrappers.<Aftersale>lambdaUpdate()
                .eq(Aftersale::getId, a.getId())
                .eq(Aftersale::getStatus, from.name())
                .set(Aftersale::getStatus, to.name());
        if (extra != null) {
            extra.accept(w);
        }
        if (aftersaleMapper.update(null, w) == 0) {
            throw new BusinessException("售后单状态已变化，请刷新后重试");
        }
        a.setStatus(to.name());
    }

    /** 售后结束（拒绝 / 撤销）后释放明细：有过成功退款则保持 REFUNDED，否则恢复 NONE */
    private void releaseItem(Long orderItemId) {
        BigDecimal refunded = Money.nz(aftersaleMapper.sumRefundedByItem(orderItemId));
        String status = refunded.signum() > 0 ? OrderItem.AS_REFUNDED : OrderItem.AS_NONE;
        orderItemMapper.update(null, Wrappers.<OrderItem>lambdaUpdate()
                .eq(OrderItem::getId, orderItemId)
                .eq(OrderItem::getAftersaleStatus, OrderItem.AS_APPLYING)
                .set(OrderItem::getAftersaleStatus, status));
    }

    private BigDecimal remainingRefundable(OrderItem item) {
        BigDecimal refunded = Money.nz(aftersaleMapper.sumRefundedByItem(item.getId()));
        return Money.scale(Money.nz(item.getPayAmount()).subtract(refunded));
    }

    private void insertWithUniqueNo(Aftersale a) {
        for (int i = 1; ; i++) {
            a.setAftersaleNo(NoGenerator.aftersaleNo());
            try {
                aftersaleMapper.insert(a);
                return;
            } catch (DuplicateKeyException e) {
                if (i >= NO_RETRY) {
                    throw new BusinessException("提交人数过多，请稍后重试");
                }
            }
        }
    }

    private Aftersale requireMemberAftersale(Long memberId, Long id) {
        Aftersale a = aftersaleMapper.selectOne(Wrappers.<Aftersale>lambdaQuery()
                .eq(Aftersale::getId, id)
                .eq(Aftersale::getMemberId, memberId));
        if (a == null) {
            throw new BusinessException("售后单不存在");
        }
        return a;
    }

    private Aftersale requireAftersale(Long id) {
        Aftersale a = aftersaleMapper.selectById(id);
        if (a == null) {
            throw new BusinessException("售后单不存在");
        }
        return a;
    }

    private PageVO<AftersaleVO> toPage(Page<Aftersale> page, boolean withMember) {
        List<Aftersale> records = page.getRecords();
        Map<Long, Order> orders = records.isEmpty() ? Map.of()
                : orderMapper.selectBatchIds(records.stream().map(Aftersale::getOrderId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Order::getId, Function.identity()));
        Map<Long, OrderItem> items = records.isEmpty() ? Map.of()
                : orderItemMapper.selectBatchIds(records.stream().map(Aftersale::getOrderItemId)
                        .collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        Map<Long, MemberBrief> members = Map.of();
        if (withMember && !records.isEmpty()) {
            Set<Long> memberIds = records.stream().map(Aftersale::getMemberId).collect(Collectors.toSet());
            members = memberApi.getMembers(memberIds);
        }
        Map<Long, MemberBrief> finalMembers = members;
        return PageVO.of(page, a -> toVO(a, orders.get(a.getOrderId()), items.get(a.getOrderItemId()),
                finalMembers.get(a.getMemberId())));
    }

    private AftersaleVO toDetail(Aftersale a, boolean withMember) {
        Order order = orderMapper.selectById(a.getOrderId());
        OrderItem item = orderItemMapper.selectById(a.getOrderItemId());
        MemberBrief m = withMember ? memberApi.getMember(a.getMemberId()) : null;
        return toVO(a, order, item, m);
    }

    private static AftersaleVO toVO(Aftersale a, Order order, OrderItem item, MemberBrief member) {
        AftersaleVO vo = new AftersaleVO();
        vo.setId(a.getId());
        vo.setAftersaleNo(a.getAftersaleNo());
        vo.setOrderId(a.getOrderId());
        vo.setOrderNo(order == null ? null : order.getOrderNo());
        vo.setOrderItemId(a.getOrderItemId());
        vo.setMemberId(a.getMemberId());
        vo.setMemberNickname(member == null ? null : member.nickname());
        vo.setType(a.getType());
        vo.setStatus(a.getStatus());
        vo.setStatusText(AftersaleStatus.textOf(a.getStatus()));
        vo.setReason(a.getReason());
        vo.setDescription(a.getDescription());
        vo.setImages(a.getImages() == null ? List.of() : a.getImages());
        vo.setAmount(a.getAmount());
        vo.setQuantity(a.getQuantity());
        vo.setAuditRemark(a.getAuditRemark());
        vo.setAuditTime(a.getAuditTime());
        vo.setReturnCompany(a.getReturnCompany());
        vo.setReturnNo(a.getReturnNo());
        vo.setRefundTime(a.getRefundTime());
        vo.setCreatedAt(a.getCreatedAt());
        vo.setItem(item == null ? null : OrderAssembler.toItemVO(item));
        return vo;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
