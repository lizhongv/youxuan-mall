package com.platform.cloud.mall.trade.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 交易领域对其他领域开放的能力：商品评价摘要、会员订单角标。
 */
public interface TradeApi {

    /** 商品详情页的评价摘要：总数、好评率（4-5 星占比，无评价时为 100）、最新 2 条显示中的评价 */
    ReviewSummary reviewSummary(Long goodsId);

    /**
     * 会员中心订单角标，键为 PENDING_PAY / PENDING_DELIVERY / DELIVERED / TO_REVIEW / AFTERSALE（进行中的售后），
     * 没有的键值为 0。
     */
    Map<String, Long> memberOrderCounts(Long memberId);

    record ReviewSummary(long total, int goodRate, List<ReviewBrief> latest) {
    }

    record ReviewBrief(Long id, String nickname, String avatar, Integer rating, String content, List<String> images,
                       String specText, LocalDateTime createdAt) {
    }
}
