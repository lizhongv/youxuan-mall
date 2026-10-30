package com.platform.cloud.mall.trade.review.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 商品详情页评价列表筛选 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PublicReviewPageQuery extends PageQuery {

    @Schema(description = "ALL 全部 / GOOD 好评(4-5) / MEDIUM 中评(3) / BAD 差评(1-2) / IMAGE 有图", defaultValue = "ALL")
    private String type;
}
