package com.platform.cloud.mall.trade.aftersale.service.impl;

import com.platform.cloud.mall.system.api.FileApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.marketing.api.CouponApi;
import com.platform.cloud.mall.marketing.api.SeckillApi;
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
import com.platform.cloud.mall.trade.common.TradeSettings.ReturnAddress;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.entity.OrderItem;
import com.platform.cloud.mall.trade.order.enums.OrderStatus;
import com.platform.cloud.mall.trade.order.mapper.OrderItemMapper;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.order.service.OrderAssembler;
import com.platform.cloud.mall.trade.order.service.OrderStateMachine;
import com.platform.cloud.mall.trade.order.service.impl.OrderServiceImpl;
import com.platform.cloud.mall.trade.pay.channel.PayChannel.RefundResult;
import com.platform.cloud.mall.trade.pay.service.PayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 *   <li>未发货订单仅退款时须全额退该商品，退款后回补库存；全部明细退完则订单取消，
 *       并与取消订单规则一致：额外退还运费、退还优惠券（已过期置 EXPIRED）、归还秒杀名额。</li>
 *   <li>确认收货后再退款：按「退款额 / 发放基数」比例回收已发放的积分与成长值（扣到 0 为止，不降级）。</li>
 *   <li>售后申请与发货都先 SELECT ... FOR UPDATE 锁订单行，二者串行。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AftersaleServiceImpl implements AftersaleService {

    private static final int NO_RETRY = 3;
    /** mbr_account_log.biz_type：退款回收订单奖励 */
    static final String BIZ_REWARD_REVOKE = "ORDER_REWARD_REVOKE";
    /** 运费退款单号后缀：渠道按退款单号幂等，运费与商品退款需不同单号 */
    static final String FREIGHT_REFUND_SUFFIX = "F";
    /** 展示退货地址的售后状态，见 {@link #showsReturnAddress} */
    static final Set<String> RETURN_ADDRESS_STATUSES = Set.of(AftersaleStatus.WAIT_RETURN.name(),
            AftersaleStatus.RETURNING.name(), AftersaleStatus.REFUNDING.name(), AftersaleStatus.REFUNDED.name());

    /** 与 trd_aftersale.audit_remark 列宽一致 */
    private static final int REMARK_MAX = 255;

    private final AftersaleMapper aftersaleMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStateMachine orderStateMachine;
    private final PayService payService;
    private final GoodsApi goodsApi;
    private final MemberApi memberApi;
    private final FileApi fileApi;
    private final TradeSettings settings;
    private final CouponApi couponApi;
    private final SeckillApi seckillApi;

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
        // 锁订单行后再判断状态：发货同样先锁订单行，保证「申请售后」与「发货」串行，
        // 不会出现发货检查通过后售后才落库、货照发钱照退的情况
        Order order = orderMapper.selectByIdForUpdate(item.getOrderId());
        if (order == null || !java.util.Objects.equals(order.getMemberId(), memberId)) {
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
        // 会员提交的图片只允许本站上传地址：防外链钓鱼、追踪像素与伪协议
        fileApi.requireUploadedImages(req.getImages());
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
        if (AftersaleType.REFUND.name().equals(a.getType())) {
            refund(a, Operators.admin(adminId), r);
        } else {
            // 买家拿到「待退货」后要按地址寄回，地址没配置就同意会让买家无处可寄、售后卡死
            if (settings.returnAddress() == null) {
                throw new BusinessException("请先在系统参数中设置退货地址");
            }
            transit(a, AftersaleStatus.WAIT_RETURN, w -> w.set(Aftersale::getAuditRemark, r)
                    .set(Aftersale::getAuditTime, now));
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onChannelRefundSuccess(String aftersaleNo) {
        Aftersale a = findByNo(aftersaleNo);
        if (a == null) {
            log.warn("退款回调找不到售后单 aftersaleNo={}", aftersaleNo);
            return;
        }
        if (!AftersaleStatus.REFUNDING.name().equals(a.getStatus())) {
            // 已完成（重复通知）或不是渠道退款中的单：幂等返回
            return;
        }
        Order order = orderMapper.selectById(a.getOrderId());
        OrderItem item = orderItemMapper.selectById(a.getOrderItemId());
        if (order == null || item == null) {
            throw new BusinessException("订单不存在");
        }
        completeRefund(a, order, item, Operators.SYSTEM, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onChannelRefundFailed(String aftersaleNo, String reason) {
        Aftersale a = findByNo(aftersaleNo);
        if (a == null || !AftersaleStatus.REFUNDING.name().equals(a.getStatus())) {
            return;
        }
        String tag = "【退款失败：" + reason + "】";
        if (a.getAuditRemark() != null && a.getAuditRemark().contains(tag)) {
            // 补偿任务会反复查到同一失败结果，备注只写一次
            return;
        }
        log.error("售后渠道退款失败，需人工处理 aftersaleNo={} reason={}", aftersaleNo, reason);
        String note = appendRemark(a.getAuditRemark(), tag);
        aftersaleMapper.update(null, Wrappers.<Aftersale>lambdaUpdate()
                .eq(Aftersale::getId, a.getId())
                .eq(Aftersale::getStatus, AftersaleStatus.REFUNDING.name())
                .set(Aftersale::getAuditRemark, note));
    }

    // ------------------------------------------------------------------ 内部

    /**
     * 执行退款：复核额度 → 售后单 REFUNDING → 按订单支付渠道原路退款 → 按渠道结果：
     * <ul>
     *   <li>SUCCESS（余额、支付宝同步退款、微信即时成功）：同一事务内完成 {@link #completeRefund}；</li>
     *   <li>OFFLINE（模拟支付）：同样完成，并在审核备注中注明「需线下处理」；</li>
     *   <li>PROCESSING（微信退款常见）/ FAILED：保持 REFUNDING，等退款回调或补偿查询。</li>
     * </ul>
     * 完成退款时：明细 REFUNDED；未发货订单回补库存，且全部明细退完时订单取消（见 {@link #cancelAfterFullRefund}）；
     * 已完成订单按比例回收确认收货时发放的积分与成长值（见 {@link #reclaimReward}）。
     * 渠道请求本身失败（网络、余额不足等）抛异常，整个事务回滚回原状态，管理员可重试；
     * 售后单号即渠道退款单号，重试不会重复退款。
     *
     * @param auditRemark 审核备注；为 null 表示不是审核动作（确认收货退款），不改审核字段
     */
    void refund(Aftersale a, String operator, String auditRemark) {
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
        transit(a, AftersaleStatus.REFUNDING, auditRemark == null ? null
                : w -> w.set(Aftersale::getAuditRemark, auditRemark).set(Aftersale::getAuditTime, now));
        if (auditRemark != null) {
            a.setAuditRemark(auditRemark);
        }
        RefundResult result = payService.refund(order, a.getAmount(), a.getAftersaleNo());
        switch (result.status()) {
            case SUCCESS -> completeRefund(a, order, item, operator, null);
            case OFFLINE -> completeRefund(a, order, item, operator,
                    appendRemark(a.getAuditRemark(), "【需线下处理】" + result.message()));
            case FAILED -> onChannelRefundFailed(a.getAftersaleNo(), result.message());
            case PROCESSING -> {
                // 等待渠道退款回调；补偿任务也会定期查询
            }
        }
    }

    /**
     * 退款完成：售后单 REFUNDED → 明细 REFUNDED；未发货订单回补库存，且全部明细退完时订单取消
     * （优惠券不退，已按实付分摊退给买家）。
     *
     * @param remark 非空时覆盖审核备注（线下处理说明）
     */
    void completeRefund(Aftersale a, Order order, OrderItem item, String operator, String remark) {
        LocalDateTime now = LocalDateTime.now();
        transit(a, AftersaleStatus.REFUNDED, w -> {
            w.set(Aftersale::getRefundTime, now);
            if (remark != null) {
                w.set(Aftersale::getAuditRemark, remark);
            }
        });
        orderItemMapper.update(null, Wrappers.<OrderItem>lambdaUpdate()
                .eq(OrderItem::getId, item.getId())
                .set(OrderItem::getAftersaleStatus, OrderItem.AS_REFUNDED));

        if (OrderStatus.PENDING_DELIVERY.name().equals(order.getStatus())) {
            goodsApi.restoreStock(item.getSkuId(), item.getQuantity());
            long notRefunded = orderItemMapper.selectCount(Wrappers.<OrderItem>lambdaQuery()
                    .eq(OrderItem::getOrderId, order.getId())
                    .ne(OrderItem::getAftersaleStatus, OrderItem.AS_REFUNDED));
            if (notRefunded == 0) {
                cancelAfterFullRefund(order, a, operator, now);
            }
        } else if (OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            reclaimReward(order, a);
        }
    }

    /**
     * 未发货订单全部明细退完 → 订单取消，规则与买家取消订单一致：
     * 运费没有分摊进明细，需单独原路退还，否则买家什么都没收到却付了运费；
     * 优惠券退还（CouponApi.giveBack 内已处理「已过期则置 EXPIRED」）；秒杀名额在本事务内归还。
     * 库存已在逐条退款时回补，这里不再回补。
     */
    void cancelAfterFullRefund(Order order, Aftersale a, String operator, LocalDateTime now) {
        String reason = "全部商品已退款";
        orderStateMachine.transit(order, OrderStatus.CANCELLED, operator, reason,
                w -> w.set(Order::getCancelTime, now).set(Order::getCancelReason, reason));
        BigDecimal freight = Money.scale(order.getFreightAmount());
        if (freight.signum() > 0) {
            payService.refund(order, freight, a.getAftersaleNo() + FREIGHT_REFUND_SUFFIX);
            orderStateMachine.log(order.getId(), OrderStatus.CANCELLED.name(), OrderStatus.CANCELLED.name(),
                    operator, "整单退款，退还运费 " + freight + " 元");
        }
        payService.markRefunded(order.getId());
        if (order.getMemberCouponId() != null) {
            couponApi.giveBack(order.getMemberCouponId());
        }
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId()));
        OrderServiceImpl.releaseSeckill(order, items, seckillApi);
    }

    /**
     * 确认收货后退款：按「本次退款 / 发放基数」比例回收积分与成长值，向下取整
     * （多次部分退款的回收总和不会超过当初发放的数量）。会员积分不足时扣到 0 并在流水注明；等级不降。
     * 升级前完成的历史订单没有记录发放数量，按当前「每元奖励」配置折算。
     */
    void reclaimReward(Order order, Aftersale a) {
        BigDecimal refund = Money.nz(a.getAmount());
        if (refund.signum() <= 0) {
            return;
        }
        int points;
        int growth;
        BigDecimal base = order.getRewardBase();
        if (base != null && order.getRewardPoints() != null && order.getRewardGrowth() != null) {
            if (base.signum() <= 0) {
                return;
            }
            BigDecimal ratio = refund.min(base);
            points = proportional(order.getRewardPoints(), ratio, base);
            growth = proportional(order.getRewardGrowth(), ratio, base);
        } else {
            points = OrderServiceImpl.rewardOf(refund, settings.pointsPerYuan());
            growth = OrderServiceImpl.rewardOf(refund, settings.growthPerYuan());
        }
        if (points > 0 || growth > 0) {
            memberApi.reclaimReward(order.getMemberId(), points, growth, BIZ_REWARD_REVOKE, a.getAftersaleNo(),
                    "订单 " + order.getOrderNo() + " 退款扣回奖励");
        }
    }

    /** granted × part / base，向下取整 */
    static int proportional(int granted, BigDecimal part, BigDecimal base) {
        if (granted <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(granted).multiply(part)
                .divide(base, 0, java.math.RoundingMode.DOWN).intValueExact();
    }

    /** audit_remark 列 255 字符 */
    static String appendRemark(String origin, String note) {
        String s = origin == null || origin.isBlank() ? note : origin + " " + note;
        return s.length() <= REMARK_MAX ? s : s.substring(0, REMARK_MAX);
    }

    private Aftersale findByNo(String aftersaleNo) {
        return aftersaleMapper.selectOne(Wrappers.<Aftersale>lambdaQuery()
                .eq(Aftersale::getAftersaleNo, aftersaleNo));
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
        // 一页只读一次配置（ConfigApi 有缓存，这里只是省掉逐行的方法调用），且只有需要展示时才读
        ReturnAddress returnAddress = records.stream().anyMatch(AftersaleServiceImpl::showsReturnAddress)
                ? settings.returnAddress() : null;
        return PageVO.of(page, a -> toVO(a, orders.get(a.getOrderId()), items.get(a.getOrderItemId()),
                finalMembers.get(a.getMemberId()), returnAddress));
    }

    private AftersaleVO toDetail(Aftersale a, boolean withMember) {
        Order order = orderMapper.selectById(a.getOrderId());
        OrderItem item = orderItemMapper.selectById(a.getOrderItemId());
        MemberBrief m = withMember ? memberApi.getMember(a.getMemberId()) : null;
        return toVO(a, order, item, m, showsReturnAddress(a) ? settings.returnAddress() : null);
    }

    /**
     * 是否向买家 / 后台展示退货地址：仅退货退款，且已审核通过（待退货、退货中）或已进入退款（便于事后核对寄往何处）。
     * 申请中、已拒绝、已撤销不展示，避免未同意退货的买家直接把货寄过来。
     */
    static boolean showsReturnAddress(Aftersale a) {
        if (!AftersaleType.RETURN_REFUND.name().equals(a.getType())) {
            return false;
        }
        return RETURN_ADDRESS_STATUSES.contains(a.getStatus());
    }

    /**
     * @param returnAddress 当前退货地址配置；本方法按售后单类型与状态决定是否输出
     */
    private static AftersaleVO toVO(Aftersale a, Order order, OrderItem item, MemberBrief member,
                                    ReturnAddress returnAddress) {
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
        vo.setReturnAddress(showsReturnAddress(a) ? returnAddress : null);
        return vo;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
