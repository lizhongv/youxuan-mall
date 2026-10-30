package com.platform.cloud.mall.trade.api.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.platform.cloud.mall.trade.aftersale.entity.Aftersale;
import com.platform.cloud.mall.trade.aftersale.enums.AftersaleStatus;
import com.platform.cloud.mall.trade.aftersale.mapper.AftersaleMapper;
import com.platform.cloud.mall.trade.api.TradeApi;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.enums.OrderStatus;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.order.mapper.row.StatusCountRow;
import com.platform.cloud.mall.trade.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** 交易领域对外能力实现：商品详情的评价摘要、会员中心订单角标 */
@Service
@RequiredArgsConstructor
public class TradeApiImpl implements TradeApi {

    static final String TO_REVIEW = "TO_REVIEW";
    static final String AFTERSALE = "AFTERSALE";

    private final ReviewService reviewService;
    private final OrderMapper orderMapper;
    private final AftersaleMapper aftersaleMapper;

    @Override
    public ReviewSummary reviewSummary(Long goodsId) {
        return reviewService.summary(goodsId);
    }

    @Override
    public Map<String, Long> memberOrderCounts(Long memberId) {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put(OrderStatus.PENDING_PAY.name(), 0L);
        result.put(OrderStatus.PENDING_DELIVERY.name(), 0L);
        result.put(OrderStatus.DELIVERED.name(), 0L);
        for (StatusCountRow r : orderMapper.countByStatusOfMember(memberId)) {
            if (result.containsKey(r.getStatus())) {
                result.put(r.getStatus(), r.getCnt());
            }
        }
        result.put(TO_REVIEW, orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .eq(Order::getMemberId, memberId)
                .eq(Order::getMemberDeleted, 0)
                .eq(Order::getStatus, OrderStatus.COMPLETED.name())
                .eq(Order::getReviewed, 0)));
        result.put(AFTERSALE, aftersaleMapper.selectCount(Wrappers.<Aftersale>lambdaQuery()
                .eq(Aftersale::getMemberId, memberId)
                .in(Aftersale::getStatus, AftersaleStatus.IN_PROGRESS)));
        return result;
    }
}
