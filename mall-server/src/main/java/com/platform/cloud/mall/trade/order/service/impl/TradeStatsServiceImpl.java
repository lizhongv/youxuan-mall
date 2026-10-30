package com.platform.cloud.mall.trade.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.platform.cloud.mall.trade.aftersale.entity.Aftersale;
import com.platform.cloud.mall.trade.aftersale.enums.AftersaleStatus;
import com.platform.cloud.mall.trade.aftersale.mapper.AftersaleMapper;
import com.platform.cloud.mall.trade.common.Money;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import com.platform.cloud.mall.trade.order.mapper.row.SalesRow;
import com.platform.cloud.mall.trade.order.service.OrderService;
import com.platform.cloud.mall.trade.order.service.TradeStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TradeStatsServiceImpl implements TradeStatsService {

    private final OrderMapper orderMapper;
    private final AftersaleMapper aftersaleMapper;
    private final OrderService orderService;

    @Override
    public SalesStat paidBetween(LocalDateTime from, LocalDateTime to) {
        SalesRow r = orderMapper.sumPaid(from, to);
        if (r == null) {
            return new SalesStat(Money.scale(null), 0);
        }
        return new SalesStat(Money.scale(r.getSales()), r.getOrders() == null ? 0 : r.getOrders());
    }

    @Override
    public List<DailySales> dailyPaid(LocalDateTime from, LocalDateTime to) {
        return orderMapper.dailyPaid(from, to).stream()
                .map(r -> new DailySales(r.getDate(), Money.scale(r.getSales()),
                        r.getOrders() == null ? 0 : r.getOrders()))
                .toList();
    }

    @Override
    public List<GoodsSales> topGoods(LocalDateTime from, int limit) {
        return orderMapper.topGoods(from, limit).stream()
                .map(r -> new GoodsSales(r.getGoodsId(), r.getGoodsName(), r.getPic(),
                        r.getQuantity() == null ? 0 : r.getQuantity(), Money.scale(r.getAmount())))
                .toList();
    }

    @Override
    public Map<String, Long> orderStatusCount() {
        return orderService.statusCount();
    }

    @Override
    public long pendingAftersale() {
        return aftersaleMapper.selectCount(Wrappers.<Aftersale>lambdaQuery()
                .in(Aftersale::getStatus, AftersaleStatus.APPLYING.name(), AftersaleStatus.RETURNING.name()));
    }
}
