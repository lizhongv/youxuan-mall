package com.platform.cloud.mall.trade.order.service;

import com.platform.cloud.mall.trade.order.dto.OrderSubmitRequest;
import com.platform.cloud.mall.trade.order.vo.OrderConfirmVO;
import com.platform.cloud.mall.trade.order.vo.OrderSubmitVO;

/** 结算与下单 */
public interface OrderCheckoutService {

    /** 结算预览：不落库、不占库存，只计算金额与可用券 */
    OrderConfirmVO confirm(Long memberId, OrderSubmitRequest req);

    /** 提交订单：校验、扣库存、核销券、生成待支付订单，整个过程一个事务 */
    OrderSubmitVO submit(Long memberId, OrderSubmitRequest req);
}
