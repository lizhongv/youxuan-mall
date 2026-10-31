package com.platform.cloud.mall.trade.pay.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.trade.common.NoGenerator;
import com.platform.cloud.mall.trade.common.Operators;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.enums.OrderStatus;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.order.service.OrderStateMachine;
import com.platform.cloud.mall.trade.pay.channel.PayChannel;
import com.platform.cloud.mall.trade.pay.channel.PayChannel.PayCommand;
import com.platform.cloud.mall.trade.pay.channel.PayChannel.PayResult;
import com.platform.cloud.mall.trade.pay.channel.PayChannel.RefundCommand;
import com.platform.cloud.mall.trade.pay.channel.PayChannel.RefundResult;
import com.platform.cloud.mall.trade.pay.entity.Payment;
import com.platform.cloud.mall.trade.pay.mapper.PaymentMapper;
import com.platform.cloud.mall.trade.pay.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付服务：按 pay_type 路由到 {@link PayChannel}，写支付流水，再用条件更新把订单置为待发货。
 * <p>
 * 并发安全：订单状态更新带 {@code WHERE status = 'PENDING_PAY'}，与关单、重复支付互斥；
 * 失败时整个事务（含余额扣款、支付流水）回滚。接口层另有 @Idempotent 挡住重复点击。
 */
@Slf4j
@Service
public class PayServiceImpl implements PayService {

    private final Map<String, PayChannel> channels = new HashMap<>();
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final OrderStateMachine stateMachine;

    public PayServiceImpl(List<PayChannel> channelList, OrderMapper orderMapper, PaymentMapper paymentMapper,
                          OrderStateMachine stateMachine) {
        for (PayChannel c : channelList) {
            channels.put(c.type(), c);
        }
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.stateMachine = stateMachine;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pay(Long memberId, Long orderId, String payType) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getId, orderId)
                .eq(Order::getMemberId, memberId));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!OrderStatus.PENDING_PAY.name().equals(order.getStatus())) {
            throw new BusinessException("订单" + OrderStatus.textOf(order.getStatus()) + "，无需支付");
        }
        LocalDateTime now = LocalDateTime.now();
        if (order.getPayExpireAt() != null && !now.isBefore(order.getPayExpireAt())) {
            throw new BusinessException("订单已超时，请重新下单");
        }
        PayChannel channel = channels.get(payType);
        if (channel == null || !channel.enabled()) {
            throw new BusinessException("暂不支持该支付方式");
        }

        Payment payment = new Payment();
        payment.setPayNo(NoGenerator.payNo());
        payment.setOrderId(order.getId());
        payment.setMemberId(memberId);
        payment.setChannel(channel.type());
        payment.setAmount(order.getPayAmount());
        payment.setStatus(Payment.WAITING);
        payment.setChannelNo("");
        paymentMapper.insert(payment);

        PayResult result = channel.pay(new PayCommand(memberId, order.getId(), order.getOrderNo(),
                payment.getPayNo(), order.getPayAmount()));
        if (!result.success()) {
            // 异步渠道：等待回调 onPaid
            return false;
        }
        markPaid(order, payment, result.channelNo(), Operators.member(memberId), now);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onPaid(String payNo, String channelNo) {
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery().eq(Payment::getPayNo, payNo));
        if (payment == null) {
            throw new BusinessException("支付单不存在");
        }
        if (Payment.SUCCESS.equals(payment.getStatus())) {
            // 渠道会重复通知，已处理过直接返回成功
            return;
        }
        Order order = orderMapper.selectById(payment.getOrderId());
        if (order == null || !OrderStatus.PENDING_PAY.name().equals(order.getStatus())) {
            // 订单已关闭后才收到付款：不能自动发货，交由人工退款
            log.error("收到支付成功通知但订单不是待付款状态，需人工退款 payNo={} orderId={}", payNo,
                    payment.getOrderId());
            throw new BusinessException("订单状态异常，需人工处理");
        }
        markPaid(order, payment, channelNo, Operators.SYSTEM, LocalDateTime.now());
    }

    @Override
    public RefundResult refund(Order order, BigDecimal amount, String refundNo) {
        // 退款不检查 enabled：渠道下线后已支付订单仍需能原路退回
        PayChannel channel = channels.get(order.getPayType());
        if (channel == null) {
            throw new BusinessException("订单支付方式不支持退款：" + order.getPayType());
        }
        return channel.refund(new RefundCommand(order.getMemberId(), order.getId(), order.getOrderNo(),
                order.getPayNo(), refundNo, amount));
    }

    @Override
    public void markRefunded(Long orderId) {
        paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                .eq(Payment::getOrderId, orderId)
                .eq(Payment::getStatus, Payment.SUCCESS)
                .set(Payment::getStatus, Payment.REFUNDED));
    }

    /**
     * 落账：订单 PENDING_PAY → PENDING_DELIVERY。
     * 条件里带 {@code pay_amount = 支付单金额}：读取订单到这里之间后台可能刚改了价，
     * 若仍按旧金额落账就成了「少付也发货」。条件不满足时抛业务异常，整个事务（含余额扣款）回滚。
     */
    private void markPaid(Order order, Payment payment, String channelNo, String operator, LocalDateTime now) {
        String channel = payment.getChannel();
        try {
            stateMachine.transit(order, OrderStatus.PENDING_DELIVERY, operator, "支付成功（" + channel + "）",
                    w -> w.eq(Order::getPayAmount, payment.getAmount())
                            .set(Order::getPayType, channel)
                            .set(Order::getPayTime, now)
                            .set(Order::getPayNo, payment.getPayNo()));
        } catch (BusinessException e) {
            Order latest = orderMapper.selectById(order.getId());
            if (latest != null && OrderStatus.PENDING_PAY.name().equals(latest.getStatus())
                    && latest.getPayAmount() != null && payment.getAmount() != null
                    && latest.getPayAmount().compareTo(payment.getAmount()) != 0) {
                log.warn("支付时订单金额已变化 orderId={} 支付单金额={} 当前应付={}", order.getId(),
                        payment.getAmount(), latest.getPayAmount());
                throw new BusinessException("订单金额已变化，请刷新后重试");
            }
            throw e;
        }
        paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                .eq(Payment::getId, payment.getId())
                .eq(Payment::getStatus, Payment.WAITING)
                .set(Payment::getStatus, Payment.SUCCESS)
                .set(Payment::getChannelNo, channelNo == null ? "" : channelNo)
                .set(Payment::getPaidAt, now));
        order.setPayType(channel);
        order.setPayTime(now);
        order.setPayNo(payment.getPayNo());
    }
}
