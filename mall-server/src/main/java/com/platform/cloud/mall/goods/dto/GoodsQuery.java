package com.platform.cloud.mall.goods.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端商品分页筛选：keyword 匹配名称 / 关键词。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GoodsQuery extends PageQuery {

    @Schema(description = "类目 ID（一级类目包含其子类目）")
    private Long categoryId;

    @Schema(description = "品牌 ID")
    private Long brandId;

    @Schema(description = "ON_SALE / OFF_SALE")
    private String status;
}
