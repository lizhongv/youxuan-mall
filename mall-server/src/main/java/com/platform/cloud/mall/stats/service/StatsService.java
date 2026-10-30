package com.platform.cloud.mall.stats.service;

import com.platform.cloud.mall.stats.vo.OrderStatusStatVO;
import com.platform.cloud.mall.stats.vo.OverviewVO;
import com.platform.cloud.mall.stats.vo.TopGoodsVO;
import com.platform.cloud.mall.stats.vo.TrendPointVO;

import java.util.List;

/** 后台首页统计 */
public interface StatsService {

    OverviewVO overview();

    /** 近 days 天（含今天）趋势，缺失日期补 0 */
    List<TrendPointVO> trend(int days);

    List<TopGoodsVO> topGoods(int days, int limit);

    List<OrderStatusStatVO> orderStatus();
}
