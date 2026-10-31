package com.platform.cloud.mall.trade.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.pay.PayType;
import com.platform.cloud.mall.trade.pay.dto.PaymentPageQuery;
import com.platform.cloud.mall.trade.pay.entity.Payment;
import com.platform.cloud.mall.trade.pay.mapper.PaymentMapper;
import com.platform.cloud.mall.trade.pay.service.PaySettlementService;
import com.platform.cloud.mall.trade.pay.service.PaymentAdminService;
import com.platform.cloud.mall.trade.pay.vo.PaymentAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentAdminServiceImpl implements PaymentAdminService {

    private static final Map<String, String> STATUS_TEXT = Map.of(
            Payment.WAITING, "待支付",
            Payment.SUCCESS, "支付成功",
            Payment.CLOSED, "已关闭",
            Payment.REFUNDED, "已退款",
            Payment.REFUNDED_AFTER_CLOSE, "关单后到账已退款");

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final PaySettlementService settlement;

    @Override
    public PageVO<PaymentAdminVO> page(PaymentPageQuery q) {
        LambdaQueryWrapper<Payment> w = Wrappers.<Payment>lambdaQuery()
                .eq(StringUtils.hasText(q.getPayNo()), Payment::getPayNo, trim(q.getPayNo()))
                .eq(StringUtils.hasText(q.getChannel()), Payment::getChannel, q.getChannel())
                .eq(StringUtils.hasText(q.getStatus()), Payment::getStatus, q.getStatus())
                .ge(q.getStartTime() != null, Payment::getCreatedAt, q.getStartTime())
                .le(q.getEndTime() != null, Payment::getCreatedAt, q.getEndTime());
        if (StringUtils.hasText(q.getOrderNo())) {
            Order o = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                    .select(Order::getId).eq(Order::getOrderNo, q.getOrderNo().trim()));
            if (o == null) {
                return PageVO.of(new Page<Payment>(q.getPage(), q.getSize()), p -> toVO(p, null));
            }
            w.eq(Payment::getOrderId, o.getId());
        }
        w.orderByDesc(Payment::getCreatedAt).orderByDesc(Payment::getId);
        Page<Payment> page = paymentMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w);
        List<Payment> records = page.getRecords();
        Map<Long, Order> orders = records.isEmpty() ? Map.of()
                : orderMapper.selectBatchIds(records.stream().map(Payment::getOrderId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Order::getId, Function.identity()));
        return PageVO.of(page, p -> toVO(p, orders.get(p.getOrderId())));
    }

    @Override
    public PaymentAdminVO sync(Long id) {
        Payment p = paymentMapper.selectById(id);
        if (p == null) {
            throw new BusinessException("支付流水不存在");
        }
        Payment latest = settlement.sync(p);
        return toVO(latest, orderMapper.selectById(latest.getOrderId()));
    }

    static PaymentAdminVO toVO(Payment p, Order order) {
        PaymentAdminVO vo = new PaymentAdminVO();
        vo.setId(p.getId());
        vo.setPayNo(p.getPayNo());
        vo.setOrderId(p.getOrderId());
        vo.setOrderNo(order == null ? null : order.getOrderNo());
        vo.setMemberId(p.getMemberId());
        vo.setChannel(p.getChannel());
        vo.setChannelText(PayType.channelText(p.getChannel()));
        vo.setTradeType(p.getTradeType());
        vo.setAmount(p.getAmount());
        vo.setStatus(p.getStatus());
        vo.setStatusText(STATUS_TEXT.getOrDefault(p.getStatus(), p.getStatus()));
        vo.setChannelNo(p.getChannelNo());
        vo.setPaidAt(p.getPaidAt());
        vo.setRemark(p.getRemark());
        vo.setCreatedAt(p.getCreatedAt());
        return vo;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
