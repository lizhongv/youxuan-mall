package com.platform.cloud.mall.trade.review.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 管理端评价筛选 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminReviewPageQuery extends PageQuery {

    private Long goodsId;

    @Schema(description = "星级 1-5")
    private Integer rating;

    @Schema(description = "1 显示 0 隐藏")
    private Integer status;

    @Schema(description = "1 只看有图 0 只看无图")
    private Integer hasImage;
}
