package com.platform.cloud.mall.trade.pay.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.member.api.MemberApi.MemberBrief;
import com.platform.cloud.mall.trade.common.NoGenerator;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.enums.OrderStatus;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.pay.PayProperties;
import com.platform.cloud.mall.trade.pay.PayType;
import com.platform.cloud.mall.trade.pay.channel.AsyncPayChannel;
import com.platform.cloud.mall.trade.pay.channel.AsyncPayChannel.Availability;
import com.platform.cloud.mall.trade.pay.channel.AsyncPayChannel.PrepayCommand;
import com.platform.cloud.mall.trade.pay.channel.ChannelTrade;
import com.platform.cloud.mall.trade.pay.config.PaySettings;
import com.platform.cloud.mall.trade.pay.entity.Payment;
import com.platform.cloud.mall.trade.pay.mapper.PaymentMapper;
import com.platform.cloud.mall.trade.pay.service.CashierService;
import com.platform.cloud.mall.trade.pay.service.PayService;
import com.platform.cloud.mall.trade.pay.service.PaySettlementService;
import com.platform.cloud.mall.trade.pay.service.PaySettlementService.SettleOutcome;
import com.platform.cloud.mall.trade.pay.support.PayAmounts;
import com.platform.cloud.mall.trade.pay.vo.PayChannelVO;
import com.platform.cloud.mall.trade.pay.vo.PayLaunchVO;
import com.platform.cloud.mall.trade.pay.vo.PayStatusVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收银台。
 * <p>
 * 异步渠道发起流程：校验订单 → 处理该订单已有的待支付流水（未过期且同方式同金额的直接复用，其余先查单再关单）
 * → 写 WAITING 流水 → 渠道下单 → 保存调起参数。方法不加事务：渠道下单是网络调用，不能占着数据库连接；
 * 流水先落库再下单，即使下单后进程崩溃，补偿任务也能凭流水查到渠道订单。
 * 接口层的 @Idempotent 挡住同一次点击的重复提交。
 */
@Slf4j
@Service
public class CashierServiceImpl implements CashierService {

    /**
     * 复用旧调起参数的时限。微信 H5 的 h5_url 有效期 5 分钟，取 4 分钟留余量；
     * JSAPI prepay_id 有效 2 小时、支付宝跳转地址也远长于此，统一用最短的，规则简单。
     */
    static final long REUSE_MINUTES = 4;
    /** 轮询查单节流：前端约 2 秒轮询一次，同一支付单 3 秒内只向渠道查一次，避免触发渠道频率限制 */
    static final Duration QUERY_THROTTLE = Duration.ofSeconds(3);
    static final String QUERY_THROTTLE_PREFIX = "mall:pay:query:";
    /** 轮询时最多查最近几笔待支付流水（同一订单正常只有一笔） */
    private static final int MAX_SYNC_PER_POLL = 3;
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {
    };

    private final Map<String, AsyncPayChannel> channels = new HashMap<>();
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final PayService payService;
    private final PaySettlementService settlement;
    private final MemberApi memberApi;
    private final PaySettings settings;
    private final PayProperties payProperties;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redis;

    public CashierServiceImpl(List<AsyncPayChannel> channelList, OrderMapper orderMapper, PaymentMapper paymentMapper,
                              PayService payService, PaySettlementService settlement, MemberApi memberApi,
                              PaySettings settings, PayProperties payProperties, ObjectMapper objectMapper,
                              StringRedisTemplate redis) {
        for (AsyncPayChannel c : channelList) {
            channels.put(c.type(), c);
        }
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.payService = payService;
        this.settlement = settlement;
        this.memberApi = memberApi;
        this.settings = settings;
        this.payProperties = payProperties;
        this.objectMapper = objectMapper;
        this.redis = redis;
    }

    @Override
    public List<PayChannelVO> channels(Long memberId, Long orderId, boolean inWechat) {
        Order order = requirePayableOrder(memberId, orderId);
        List<PayChannelVO> list = new ArrayList<>();

        MemberBrief m = memberApi.getMember(memberId);
        BigDecimal balance = m == null || m.balance() == null ? BigDecimal.ZERO : m.balance();
        boolean enough = balance.compareTo(order.getPayAmount()) >= 0;
        list.add(vo(PayType.BALANCE, enough, enough ? "" : "余额不足（可用 " + balance.toPlainString() + " 元）"));

        String openid = null;
        for (PayType t : List.of(PayType.WECHAT_JSAPI, PayType.WECHAT_H5, PayType.ALIPAY_WAP)) {
            Availability av = availability(t);
            if (!av.on()) {
                // 后台关闭的方式不展示
                continue;
            }
            if (!av.ready()) {
                list.add(vo(t, false, av.reason()));
                continue;
            }
            String reason = "";
            if (t == PayType.WECHAT_JSAPI) {
                if (!inWechat) {
                    reason = "请在微信中打开商城使用微信支付";
                } else {
                    openid = openid == null ? memberApi.getOpenid(memberId, MemberApi.PLATFORM_WECHAT_MP) : openid;
                    if (!StringUtils.hasText(openid)) {
                        reason = "请先通过微信授权登录后使用";
                    }
                }
            } else if (t == PayType.WECHAT_H5 && inWechat) {
                // 微信内置浏览器不支持 H5 支付，应使用公众号支付
                reason = "微信内请使用公众号支付";
            } else if (t == PayType.ALIPAY_WAP && inWechat) {
                // 微信屏蔽支付宝跳转，主流做法是提示用户在浏览器中打开
                reason = "微信内无法使用支付宝，请点击右上角在浏览器中打开";
            }
            list.add(vo(t, reason.isEmpty(), reason));
        }
        if (payProperties.isMockEnabled()) {
            list.add(vo(PayType.MOCK, true, ""));
        }
        return list;
    }

    @Override
    public PayLaunchVO pay(Long memberId, Long orderId, String payTypeName, String clientIp) {
        PayType type = PayType.of(payTypeName);
        Order order = requirePayableOrder(memberId, orderId);
        if (type.sync()) {
            if (type == PayType.MOCK && !payProperties.isMockEnabled()) {
                throw new BusinessException("暂不支持该支付方式");
            }
            // 先处理掉之前发起的微信 / 支付宝待支付单：若其实已付款，这里会落账并提示，避免再扣一次余额
            closeOutstanding(order, null);
            payService.pay(memberId, orderId, type.channel());
            return PayLaunchVO.paid(type.name());
        }

        AsyncPayChannel ch = channels.get(type.channel());
        Availability av = availability(type);
        if (ch == null || !av.ready()) {
            throw new BusinessException(av.on() ? av.reason() : "暂不支持该支付方式");
        }
        String openid = null;
        if (type == PayType.WECHAT_JSAPI) {
            openid = memberApi.getOpenid(memberId, MemberApi.PLATFORM_WECHAT_MP);
            if (!StringUtils.hasText(openid)) {
                throw new BusinessException("请先通过微信授权登录后使用微信支付");
            }
        }
        Payment reusable = closeOutstanding(order, type);
        if (reusable != null) {
            return PayLaunchVO.pending(type.name(), reusable.getPayNo(), readParams(reusable.getPayParams()));
        }

        Payment p = new Payment();
        p.setPayNo(NoGenerator.payNo());
        p.setOrderId(order.getId());
        p.setMemberId(memberId);
        p.setChannel(type.channel());
        p.setTradeType(type.name());
        p.setAmount(order.getPayAmount());
        p.setStatus(Payment.WAITING);
        p.setChannelNo("");
        p.setRemark("");
        paymentMapper.insert(p);

        Map<String, String> params;
        try {
            params = ch.prepay(new PrepayCommand(type, p.getPayNo(), order.getOrderNo(), order.getPayAmount(),
                    subject(order), order.getPayExpireAt(), openid, clientIp, settings.returnUrl(order.getId())));
        } catch (RuntimeException e) {
            close(p, "渠道下单失败");
            throw e instanceof BusinessException be ? be : new BusinessException("支付下单失败，请稍后重试");
        }
        paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                .eq(Payment::getId, p.getId())
                .set(Payment::getPayParams, writeParams(params)));
        return PayLaunchVO.pending(type.name(), p.getPayNo(), params);
    }

    @Override
    public PayStatusVO payStatus(Long memberId, Long orderId) {
        Order order = requireOwnOrder(memberId, orderId);
        if (OrderStatus.PENDING_PAY.name().equals(order.getStatus())) {
            List<Payment> waiting = paymentMapper.selectList(Wrappers.<Payment>lambdaQuery()
                    .eq(Payment::getOrderId, orderId)
                    .eq(Payment::getStatus, Payment.WAITING)
                    .in(Payment::getChannel, channels.keySet())
                    .orderByDesc(Payment::getCreatedAt));
            boolean synced = false;
            for (Payment p : waiting.subList(0, Math.min(MAX_SYNC_PER_POLL, waiting.size()))) {
                if (!acquireQuery(p.getPayNo())) {
                    continue;
                }
                try {
                    settlement.sync(p);
                    synced = true;
                } catch (RuntimeException e) {
                    // 查单失败不影响轮询：回调或补偿任务还会处理
                    log.warn("轮询查单失败 payNo={} {}", p.getPayNo(), e.getMessage());
                }
            }
            if (synced) {
                order = requireOwnOrder(memberId, orderId);
            }
        }
        PayStatusVO vo = new PayStatusVO();
        vo.setOrderId(order.getId());
        vo.setStatus(order.getStatus());
        vo.setStatusText(OrderStatus.textOf(order.getStatus()));
        vo.setPaid(order.getPayTime() != null);
        vo.setPayType(order.getPayType());
        vo.setPayTime(order.getPayTime());
        return vo;
    }

    /**
     * 处理订单已有的 WAITING 流水，避免同一订单在多个渠道同时开着可付款的订单：
     * 同方式、同金额、未超过复用时限且有调起参数的返回以复用；其余先查单——已付款则落账并提示，
     * 未付款则关闭渠道订单再关闭本地流水。查单 / 关单失败时宁可让用户稍后再试，也不冒重复扣款的风险。
     *
     * @param reuseType 想复用的支付方式；同步渠道传 null（不复用，全部关闭）
     */
    Payment closeOutstanding(Order order, PayType reuseType) {
        List<Payment> waiting = paymentMapper.selectList(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderId, order.getId())
                .eq(Payment::getStatus, Payment.WAITING)
                .orderByDesc(Payment::getCreatedAt));
        Payment reusable = null;
        LocalDateTime reuseAfter = LocalDateTime.now().minusMinutes(REUSE_MINUTES);
        for (Payment p : waiting) {
            if (reusable == null && reuseType != null && reuseType.name().equals(p.getTradeType())
                    && StringUtils.hasText(p.getPayParams())
                    && p.getCreatedAt() != null && p.getCreatedAt().isAfter(reuseAfter)
                    && PayAmounts.toFen(p.getAmount()) == PayAmounts.toFen(order.getPayAmount())) {
                reusable = p;
                continue;
            }
            AsyncPayChannel ch = channels.get(p.getChannel());
            if (ch == null) {
                close(p, "重新发起支付");
                continue;
            }
            ChannelTrade t;
            try {
                t = ch.query(p);
            } catch (RuntimeException e) {
                log.warn("重新发起支付前查单失败 payNo={} {}", p.getPayNo(), e.getMessage());
                throw new BusinessException("上一笔支付状态确认中，请稍后再试");
            }
            if (t.paid()) {
                SettleOutcome outcome = settlement.settle(t);
                if (outcome != SettleOutcome.REFUNDED_AFTER_CLOSE) {
                    throw new BusinessException("订单已支付，请勿重复支付");
                }
                continue;
            }
            if (t.state() == ChannelTrade.State.WAITING) {
                try {
                    ch.close(p);
                } catch (RuntimeException e) {
                    log.warn("重新发起支付前关单失败 payNo={} {}", p.getPayNo(), e.getMessage());
                    throw new BusinessException("上一笔支付状态确认中，请稍后再试");
                }
            }
            close(p, "重新发起支付，关闭旧支付单");
        }
        return reusable;
    }

    private Availability availability(PayType t) {
        AsyncPayChannel ch = channels.get(t.channel());
        return ch == null ? Availability.off() : ch.availability(t);
    }

    private Order requireOwnOrder(Long memberId, Long orderId) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getId, orderId)
                .eq(Order::getMemberId, memberId));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private Order requirePayableOrder(Long memberId, Long orderId) {
        Order order = requireOwnOrder(memberId, orderId);
        if (!OrderStatus.PENDING_PAY.name().equals(order.getStatus())) {
            throw new BusinessException("订单" + OrderStatus.textOf(order.getStatus()) + "，无需支付");
        }
        if (order.getPayExpireAt() != null && !LocalDateTime.now().isBefore(order.getPayExpireAt())) {
            throw new BusinessException("订单已超时，请重新下单");
        }
        return order;
    }

    private void close(Payment p, String remark) {
        paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                .eq(Payment::getId, p.getId())
                .eq(Payment::getStatus, Payment.WAITING)
                .set(Payment::getStatus, Payment.CLOSED)
                .set(Payment::getRemark, remark));
    }

    /** Redis 不可用时放行：节流只是保护，不能因此让用户看不到支付结果 */
    private boolean acquireQuery(String payNo) {
        try {
            return !Boolean.FALSE.equals(redis.opsForValue().setIfAbsent(QUERY_THROTTLE_PREFIX + payNo, "1",
                    QUERY_THROTTLE));
        } catch (RuntimeException e) {
            return true;
        }
    }

    private String subject(Order order) {
        String name = settings.mallName();
        return (StringUtils.hasText(name) ? name : "商城") + " 订单 " + order.getOrderNo();
    }

    private String writeParams(Map<String, String> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private Map<String, String> readParams(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new BusinessException("支付参数异常，请稍后重试");
        }
    }

    private static PayChannelVO vo(PayType t, boolean enabled, String reason) {
        return new PayChannelVO(t.name(), t.text(), t.icon(), enabled, reason);
    }
}
