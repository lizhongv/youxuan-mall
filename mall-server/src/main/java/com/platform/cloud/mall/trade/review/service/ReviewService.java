package com.platform.cloud.mall.trade.review.service;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.trade.api.TradeApi.ReviewSummary;
import com.platform.cloud.mall.trade.review.dto.AdminReviewPageQuery;
import com.platform.cloud.mall.trade.review.dto.PublicReviewPageQuery;
import com.platform.cloud.mall.trade.review.dto.ReviewCreateRequest;
import com.platform.cloud.mall.trade.review.vo.ReviewStatVO;
import com.platform.cloud.mall.trade.review.vo.ReviewVO;

/** 商品评价 */
public interface ReviewService {

    Long create(Long memberId, ReviewCreateRequest req);

    PageVO<ReviewVO> minePage(Long memberId, PageQuery query);

    PageVO<ReviewVO> publicPage(Long goodsId, PublicReviewPageQuery query);

    ReviewStatVO stat(Long goodsId);

    ReviewSummary summary(Long goodsId);

    PageVO<ReviewVO> adminPage(AdminReviewPageQuery query);

    void reply(Long adminId, Long id, String reply);

    void updateStatus(Long adminId, Long id, int status);
}
