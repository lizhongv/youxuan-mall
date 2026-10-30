package com.platform.cloud.mall.stats.service.impl;

import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.stats.service.StatsService;
import com.platform.cloud.mall.stats.vo.OrderStatusStatVO;
import com.platform.cloud.mall.stats.vo.OverviewVO;
import com.platform.cloud.mall.stats.vo.TopGoodsVO;
import com.platform.cloud.mall.stats.vo.TrendPointVO;
import com.platform.cloud.mall.trade.order.enums.OrderStatus;
import com.platform.cloud.mall.trade.order.service.TradeStatsService;
import com.platform.cloud.mall.trade.order.service.TradeStatsService.DailySales;
import com.platform.cloud.mall.trade.order.service.TradeStatsService.SalesStat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台统计。销售口径：按支付时间统计已支付订单（pay_time 非空），未支付即取消的订单不计入。
 * 会员数来自会员领域，在售商品数来自商品领域，统计领域本身不访问任何表。
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    /** 统计「累计会员」的起点：早于系统上线即可 */
    private static final LocalDateTime EPOCH = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final TradeStatsService tradeStats;
    private final MemberApi memberApi;
    private final GoodsApi goodsApi;

    @Override
    public OverviewVO overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);

        SalesStat t = tradeStats.paidBetween(todayStart, tomorrowStart);
        SalesStat y = tradeStats.paidBetween(yesterdayStart, todayStart);
        Map<String, Long> status = tradeStats.orderStatusCount();
        return new OverviewVO(
                t.sales(), t.orders(), memberApi.countRegistered(todayStart, tomorrowStart),
                y.sales(), y.orders(),
                memberApi.countRegistered(EPOCH, tomorrowStart),
                goodsApi.countOnSale(),
                status.getOrDefault(OrderStatus.PENDING_DELIVERY.name(), 0L),
                tradeStats.pendingAftersale(),
                status.getOrDefault(OrderStatus.PENDING_PAY.name(), 0L));
    }

    @Override
    public List<TrendPointVO> trend(int days) {
        return trend(days, LocalDate.now());
    }

    /** 可指定「今天」，便于单测 */
    List<TrendPointVO> trend(int days, LocalDate today) {
        LocalDate first = today.minusDays(days - 1L);
        LocalDateTime from = first.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        Map<String, DailySales> sales = tradeStats.dailyPaid(from, to).stream()
                .collect(Collectors.toMap(DailySales::date, Function.identity(), (a, b) -> a));
        List<TrendPointVO> result = new ArrayList<>(days);
        for (LocalDate d = first; !d.isAfter(today); d = d.plusDays(1)) {
            String key = d.toString();
            DailySales s = sales.get(key);
            // 会员领域只提供区间计数，按天逐个查询；最多 30 次，后台首页可接受
            long members = memberApi.countRegistered(d.atStartOfDay(), d.plusDays(1).atStartOfDay());
            result.add(new TrendPointVO(key,
                    s == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : s.sales(),
                    s == null ? 0 : s.orders(), members));
        }
        return result;
    }

    @Override
    public List<TopGoodsVO> topGoods(int days, int limit) {
        LocalDateTime from = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        return tradeStats.topGoods(from, limit).stream()
                .map(g -> new TopGoodsVO(g.goodsId(), g.goodsName(), g.pic(), g.quantity(), g.amount()))
                .toList();
    }

    @Override
    public List<OrderStatusStatVO> orderStatus() {
        Map<String, Long> counts = tradeStats.orderStatusCount();
        List<OrderStatusStatVO> result = new ArrayList<>();
        for (OrderStatus s : OrderStatus.values()) {
            result.add(new OrderStatusStatVO(s.name(), s.text(), counts.getOrDefault(s.name(), 0L)));
        }
        return result;
    }
}
