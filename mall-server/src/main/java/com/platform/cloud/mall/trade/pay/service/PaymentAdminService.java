package com.platform.cloud.mall.trade.pay.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.trade.pay.dto.PaymentPageQuery;
import com.platform.cloud.mall.trade.pay.vo.PaymentAdminVO;

/** 后台支付流水：查询与手动查单同步（对账、排查回调丢失） */
public interface PaymentAdminService {

    PageVO<PaymentAdminVO> page(PaymentPageQuery query);

    /** 向渠道查单并按结果落账 / 关闭；同步渠道（余额、模拟）无需查单，原样返回 */
    PaymentAdminVO sync(Long id);
}
