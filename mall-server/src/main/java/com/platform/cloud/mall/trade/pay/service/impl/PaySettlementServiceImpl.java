package com.platform.cloud.mall.trade.pay.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.trade.aftersale.entity.Aftersale;
import com.platform.cloud.mall.trade.aftersale.enums.AftersaleStatus;
import com.platform.cloud.mall.trade.aftersale.mapper.AftersaleMapper;
import com.platform.cloud.mall.trade.aftersale.service.AftersaleService;
import com.platform.cloud.mall.trade.common.Operators;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.enums.OrderStatus;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.order.service.OrderStateMachine;
import com.platform.cloud.mall.trade.pay.PayType;
import com.platform.cloud.mall.trade.pay.channel.AsyncPayChannel;
import com.platform.cloud.mall.trade.pay.channel.ChannelTrade;
import com.platform.cloud.mall.trade.pay.channel.PayChannel.RefundCommand;
import com.platform.cloud.mall.trade.pay.channel.PayChannel.RefundResult;
import com.platform.cloud.mall.trade.pay.channel.PayChannel.RefundStatus;
import com.platform.cloud.mall.trade.pay.entity.Payment;
import com.platform.cloud.mall.trade.pay.mapper.PaymentMapper;
import com.platform.cloud.mall.trade.pay.service.PayService;
import com.platform.cloud.mall.trade.pay.service.PaySettlementService;
import com.platform.cloud.mall.trade.pay.support.PayAmounts;
import com.platform.cloud.mall.trade.pay.support.PayNotifyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 异步渠道结算。
 * <p>
 * 本类方法刻意不加事务：渠道查单 / 退款是网络调用，不应占着数据库连接；
 * 落账复用 {@link PayService#onPaid}（自身事务，订单条件更新 + 流水条件更新），其余写操作都是单条条件更新，
 * 并发的回调、轮询、补偿任务之间靠「WHERE status = 原状态」互斥，重复执行结果一致。
 */
@Slf4j
@Service
public class PaySettlementServiceImpl implements PaySettlementService {

    /** 发起后多久仍未收到回调才主动查单：微信 / 支付宝回调通常秒级到达，5 分钟未到大概率是回调丢失或用户未付 */
    static final long STALE_MINUTES = 5;
    /** 补偿查询的回看窗口：订单最长支付时间远小于此，超出说明早已关闭或需人工排查 */
    static final long LOOKBACK_DAYS = 3;
    private static final Set<String> FINAL_STATUSES = Set.of(Payment.SUCCESS, Payment.REFUNDED,
            Payment.REFUNDED_AFTER_CLOSE);
    private static final int REMARK_MAX = 255;

    private final Map<String, AsyncPayChannel> channels = new HashMap<>();
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final OrderStateMachine stateMachine;
    private final PayService payService;
    private final AftersaleService aftersaleService;
    private final AftersaleMapper aftersaleMapper;

    public PaySettlementServiceImpl(List<AsyncPayChannel> channelList, PaymentMapper paymentMapper,
                                    OrderMapper orderMapper, OrderStateMachine stateMachine, PayService payService,
                                    AftersaleService aftersaleService, AftersaleMapper aftersaleMapper) {
        for (AsyncPayChannel c : channelList) {
            channels.put(c.type(), c);
        }
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
        this.stateMachine = stateMachine;
        this.payService = payService;
        this.aftersaleService = aftersaleService;
        this.aftersaleMapper = aftersaleMapper;
    }

    @Override
    public SettleOutcome settle(ChannelTrade trade) {
        if (!trade.paid()) {
            return SettleOutcome.IGNORED;
        }
        Payment p = findByPayNo(trade.payNo());
        if (p == null) {
            throw new PayNotifyException("支付单不存在 payNo=" + trade.payNo());
        }
        if (FINAL_STATUSES.contains(p.getStatus())) {
            return SettleOutcome.ALREADY;
        }
        long expected = PayAmounts.toFen(p.getAmount());
        if (trade.amountFen() != expected) {
            // 金额不符绝不落账：可能是伪造通知或渠道侧数据异常，留错误日志等人工核对
            log.error("渠道支付金额与支付流水不符，拒绝落账 payNo={} 流水={}分 渠道={}分", p.getPayNo(), expected,
                    trade.amountFen());
            throw new PayNotifyException("支付金额不符");
        }
        Order order = orderMapper.selectById(p.getOrderId());
        if (payable(order, p)) {
            if (Payment.CLOSED.equals(p.getStatus())) {
                // 用户换了支付方式后又在旧页面付了款（关单前一刻），订单仍待付款：恢复这笔流水并照常落账
                reopen(p);
            }
            try {
                payService.onPaid(p.getPayNo(), trade.channelNo());
                log.info("支付成功 payNo={} channel={} channelNo={}", p.getPayNo(), p.getChannel(),
                        trade.channelNo());
                return SettleOutcome.PAID;
            } catch (BusinessException e) {
                // 与关单 / 另一笔支付并发：重新读取后按「关单后到账」处理
                Payment again = findByPayNo(p.getPayNo());
                if (again != null && FINAL_STATUSES.contains(again.getStatus())) {
                    return SettleOutcome.ALREADY;
                }
                order = orderMapper.selectById(p.getOrderId());
                if (payable(order, again == null ? p : again)) {
                    throw e;
                }
                p = again == null ? p : again;
            }
        }
        return refundAfterClose(p, order, trade.channelNo());
    }

    @Override
    public Payment sync(Payment payment) {
        if (!Payment.WAITING.equals(payment.getStatus()) && !Payment.CLOSED.equals(payment.getStatus())) {
            return payment;
        }
        AsyncPayChannel ch = channels.get(payment.getChannel());
        if (ch == null) {
            return payment;
        }
        ChannelTrade t = ch.query(payment);
        Order order = orderMapper.selectById(payment.getOrderId());
        boolean waiting = Payment.WAITING.equals(payment.getStatus());
        switch (t.state()) {
            case PAID -> settle(t);
            case WAITING -> {
                if (waiting && !payable(order, payment)) {
                    // 订单已关闭但渠道订单还开着：关掉，防止用户在旧的支付页面继续付款
                    ch.close(payment);
                    markClosed(payment, "订单已不可支付，已关闭渠道订单");
                }
            }
            case CLOSED -> {
                if (waiting) {
                    markClosed(payment, "渠道订单已关闭");
                }
            }
            case NOT_EXIST -> {
                if (waiting && !payable(order, payment)) {
                    markClosed(payment, "订单已不可支付，渠道未创建交易");
                }
            }
            case REFUNDED -> log.warn("渠道显示该支付已退款但本地未支付，请人工核对 payNo={}", payment.getPayNo());
        }
        Payment latest = findByPayNo(payment.getPayNo());
        return latest == null ? payment : latest;
    }

    @Override
    public void onRefundResult(String refundNo, RefundResult result) {
        if (refundNo == null) {
            return;
        }
        if (refundNo.startsWith(AFTER_CLOSE_REFUND_PREFIX)) {
            String payNo = refundNo.substring(AFTER_CLOSE_REFUND_PREFIX.length());
            if (result.status() == RefundStatus.FAILED) {
                log.error("关单后到账的自动退款失败，需到商户平台人工退款 payNo={} reason={}", payNo, result.message());
                paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                        .eq(Payment::getPayNo, payNo)
                        .set(Payment::getRemark, truncate("自动退款失败，需人工处理：" + result.message())));
            } else {
                log.info("关单后到账的自动退款结果 payNo={} status={}", payNo, result.status());
            }
            return;
        }
        switch (result.status()) {
            case SUCCESS -> aftersaleService.onChannelRefundSuccess(refundNo);
            case FAILED -> aftersaleService.onChannelRefundFailed(refundNo, result.message());
            default -> {
                // PROCESSING：继续等待
            }
        }
    }

    @Override
    public int compensateWaiting(int batch) {
        LocalDateTime now = LocalDateTime.now();
        List<Payment> list = paymentMapper.selectPage(new Page<>(1, batch, false), Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getStatus, Payment.WAITING)
                .in(Payment::getChannel, channels.keySet())
                .le(Payment::getCreatedAt, now.minusMinutes(STALE_MINUTES))
                .ge(Payment::getCreatedAt, now.minusDays(LOOKBACK_DAYS))
                .orderByAsc(Payment::getCreatedAt)).getRecords();
        int ok = 0;
        for (Payment p : list) {
            try {
                sync(p);
                ok++;
            } catch (RuntimeException e) {
                log.warn("支付补偿查单失败 payNo={} {}", p.getPayNo(), e.getMessage());
            }
        }
        return ok;
    }

    @Override
    public int syncRefunding(int batch) {
        LocalDateTime now = LocalDateTime.now();
        List<Aftersale> list = aftersaleMapper.selectPage(new Page<>(1, batch, false),
                Wrappers.<Aftersale>lambdaQuery()
                        .eq(Aftersale::getStatus, AftersaleStatus.REFUNDING.name())
                        .le(Aftersale::getUpdatedAt, now.minusMinutes(STALE_MINUTES))
                        .ge(Aftersale::getUpdatedAt, now.minusDays(LOOKBACK_DAYS))
                        .orderByAsc(Aftersale::getUpdatedAt)).getRecords();
        int ok = 0;
        for (Aftersale a : list) {
            try {
                Order order = orderMapper.selectById(a.getOrderId());
                AsyncPayChannel ch = order == null ? null : channels.get(order.getPayType());
                if (ch == null) {
                    continue;
                }
                onRefundResult(a.getAftersaleNo(), ch.queryRefund(order.getPayNo(), a.getAftersaleNo()));
                ok++;
            } catch (RuntimeException e) {
                log.warn("退款补偿查询失败 aftersaleNo={} {}", a.getAftersaleNo(), e.getMessage());
            }
        }
        return ok;
    }

    /**
     * 关单后到账 / 重复支付 / 订单金额已变更：钱不能留在商户账上，自动原路退回（主流商城做法），并留错误日志与订单日志。
     * <p>
     * 先用条件更新把流水「占」成 REFUNDED_AFTER_CLOSE，保证并发的回调与补偿只有一个会发起退款；
     * 退款请求失败则把状态还原，让渠道重试通知或下一轮补偿再来一次（退款单号固定，渠道侧幂等）。
     */
    SettleOutcome refundAfterClose(Payment p, Order order, String channelNo) {
        String reason = afterCloseReason(order, p);
        String prev = p.getStatus();
        int rows = paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                .eq(Payment::getId, p.getId())
                .eq(Payment::getStatus, prev)
                .set(Payment::getStatus, Payment.REFUNDED_AFTER_CLOSE)
                .set(Payment::getChannelNo, channelNo == null ? "" : channelNo)
                .set(Payment::getPaidAt, LocalDateTime.now())
                .set(Payment::getRemark, truncate(reason + "，已自动原路退款")));
        if (rows == 0) {
            return SettleOutcome.ALREADY;
        }
        AsyncPayChannel ch = channels.get(p.getChannel());
        String refundNo = AFTER_CLOSE_REFUND_PREFIX + p.getPayNo();
        String orderNo = order == null ? "" : order.getOrderNo();
        RefundResult r;
        try {
            r = ch.refund(new RefundCommand(p.getMemberId(), p.getOrderId(), orderNo, p.getPayNo(), refundNo,
                    p.getAmount()));
        } catch (RuntimeException e) {
            paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                    .eq(Payment::getId, p.getId())
                    .eq(Payment::getStatus, Payment.REFUNDED_AFTER_CLOSE)
                    .set(Payment::getStatus, prev)
                    .set(Payment::getRemark, truncate(reason + "，自动退款失败待重试：" + e.getMessage())));
            log.error("【关单后到账】自动退款失败，等待重试 payNo={} orderNo={} amount={}", p.getPayNo(), orderNo,
                    p.getAmount(), e);
            throw new BusinessException("关单后到账自动退款失败，等待重试");
        }
        log.error("【关单后到账】{}，已自动原路退款 payNo={} orderNo={} channel={} amount={} refundNo={} 退款状态={}",
                reason, p.getPayNo(), orderNo, p.getChannel(), p.getAmount(), refundNo, r.status());
        if (order != null) {
            stateMachine.log(order.getId(), order.getStatus(), order.getStatus(), Operators.SYSTEM,
                    reason + "：收到" + PayType.channelText(p.getChannel()) + "付款 " + p.getAmount()
                            + " 元（支付单 " + p.getPayNo() + "），已自动原路退款");
        }
        return SettleOutcome.REFUNDED_AFTER_CLOSE;
    }

    /** 订单仍可用这笔流水完成支付：待付款且金额与流水一致（后台改价后旧的预支付单金额就对不上了） */
    static boolean payable(Order order, Payment p) {
        return order != null
                && OrderStatus.PENDING_PAY.name().equals(order.getStatus())
                && order.getPayAmount() != null
                && PayAmounts.toFen(order.getPayAmount()) == PayAmounts.toFen(p.getAmount());
    }

    private static String afterCloseReason(Order order, Payment p) {
        if (order == null) {
            return "订单不存在";
        }
        if (OrderStatus.PENDING_PAY.name().equals(order.getStatus())) {
            return "订单金额已变更";
        }
        if (OrderStatus.CANCELLED.name().equals(order.getStatus()) && order.getPayTime() == null) {
            return "订单已关闭后到账";
        }
        return p.getPayNo().equals(order.getPayNo()) ? "订单状态异常" : "订单已通过其他支付单付款（重复支付）";
    }

    private void reopen(Payment p) {
        paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                .eq(Payment::getId, p.getId())
                .eq(Payment::getStatus, Payment.CLOSED)
                .set(Payment::getStatus, Payment.WAITING));
        p.setStatus(Payment.WAITING);
    }

    private void markClosed(Payment p, String remark) {
        paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                .eq(Payment::getId, p.getId())
                .eq(Payment::getStatus, Payment.WAITING)
                .set(Payment::getStatus, Payment.CLOSED)
                .set(Payment::getRemark, truncate(remark)));
    }

    private Payment findByPayNo(String payNo) {
        return paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery().eq(Payment::getPayNo, payNo));
    }

    private static String truncate(String s) {
        return s.length() <= REMARK_MAX ? s : s.substring(0, REMARK_MAX);
    }
}
