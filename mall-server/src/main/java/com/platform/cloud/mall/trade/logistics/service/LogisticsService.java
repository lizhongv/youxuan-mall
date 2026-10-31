package com.platform.cloud.mall.trade.logistics.service;

import com.platform.cloud.mall.trade.logistics.vo.LogisticsVO;

/**
 * 订单物流轨迹查询。
 */
public interface LogisticsService {

    /** 会员查询自己的订单物流；订单不属于该会员时按「订单不存在」处理，不泄露订单是否存在 */
    LogisticsVO memberLogistics(Long memberId, Long orderId);

    /** 管理端查询任意订单物流 */
    LogisticsVO adminLogistics(Long orderId);
}
