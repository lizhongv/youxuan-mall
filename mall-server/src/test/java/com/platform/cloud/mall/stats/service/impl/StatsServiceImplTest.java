package com.platform.cloud.mall.stats.service.impl;

import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.stats.vo.TrendPointVO;
import com.platform.cloud.mall.trade.order.service.TradeStatsService;
import com.platform.cloud.mall.trade.order.service.TradeStatsService.DailySales;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock
    TradeStatsService tradeStats;
    @Mock
    MemberApi memberApi;
    @Mock
    GoodsApi goodsApi;
    @InjectMocks
    StatsServiceImpl service;

    @Test
    void 趋势缺失日期补0() {
        LocalDate today = LocalDate.of(2026, 10, 31);
        when(tradeStats.dailyPaid(any(), any())).thenReturn(List.of(
                new DailySales("2026-10-29", new BigDecimal("100.00"), 2)));
        when(memberApi.countRegisteredDaily(any(), any())).thenReturn(Map.of("2026-10-30", 3L));

        List<TrendPointVO> trend = service.trend(7, today);

        assertThat(trend).hasSize(7);
        assertThat(trend.get(0).date()).isEqualTo("2026-10-25");
        assertThat(trend.get(6).date()).isEqualTo("2026-10-31");
        assertThat(trend.get(4).sales()).isEqualByComparingTo("100.00");
        assertThat(trend.get(4).orders()).isEqualTo(2);
        assertThat(trend.get(5).sales()).isEqualByComparingTo("0");
        assertThat(trend.get(5).orders()).isZero();
        assertThat(trend.get(5).members()).isEqualTo(3L);
        assertThat(trend.get(4).members()).isZero();
        // 销售与新会员各一次分组查询，不按天逐个查
        verify(tradeStats, times(1)).dailyPaid(any(), any());
        verify(memberApi, times(1)).countRegisteredDaily(any(), any());
        verify(memberApi, never()).countRegistered(any(), any());
    }
}
