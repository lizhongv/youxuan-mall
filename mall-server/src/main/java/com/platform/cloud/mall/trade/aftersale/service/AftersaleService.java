package com.platform.cloud.mall.trade.aftersale.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.trade.aftersale.dto.AftersaleApplyRequest;
import com.platform.cloud.mall.trade.aftersale.dto.AftersalePageQuery;
import com.platform.cloud.mall.trade.aftersale.dto.AftersaleReturnRequest;
import com.platform.cloud.mall.trade.aftersale.vo.AftersaleVO;

/** 售后：申请、审核、退货、退款 */
public interface AftersaleService {

    Long apply(Long memberId, AftersaleApplyRequest req);

    PageVO<AftersaleVO> memberPage(Long memberId, AftersalePageQuery query);

    AftersaleVO memberDetail(Long memberId, Long id);

    void memberCancel(Long memberId, Long id);

    void memberReturn(Long memberId, Long id, AftersaleReturnRequest req);

    PageVO<AftersaleVO> adminPage(AftersalePageQuery query);

    AftersaleVO adminDetail(Long id);

    void approve(Long adminId, Long id, String remark);

    void reject(Long adminId, Long id, String remark);

    void receiveReturn(Long adminId, Long id);

    /** 渠道异步退款成功（退款回调 / 补偿查询）：REFUNDING → REFUNDED，重复通知幂等 */
    void onChannelRefundSuccess(String aftersaleNo);

    /** 渠道退款失败：保持 REFUNDING，在审核备注里写明原因，提示客服到商户平台处理 */
    void onChannelRefundFailed(String aftersaleNo, String reason);
}
