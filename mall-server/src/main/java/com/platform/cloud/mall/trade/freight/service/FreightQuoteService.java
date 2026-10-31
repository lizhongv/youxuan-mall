package com.platform.cloud.mall.trade.freight.service;

import com.platform.cloud.mall.trade.freight.support.FreightCalculator.Item;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结算运费报价：加载商品涉及的模板与默认模板，交给 {@link com.platform.cloud.mall.trade.freight.support.FreightCalculator} 计算。
 * 结算预览与提交订单共用，保证两边运费一致。
 */
public interface FreightQuoteService {

    /**
     * @param province 收货省份；未选地址时为 null（按默认地区规则预估）
     * @throws com.platform.cloud.bootstrap.common.exception.BusinessException 地址不在配送范围
     */
    BigDecimal quote(String province, List<Item> items);
}
