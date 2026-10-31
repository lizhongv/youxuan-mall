package com.platform.cloud.bootstrap.goods.spu.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.goods.spu.GoodsStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品分页查询参数")
public class GoodsQuery extends PageQuery {

    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "品牌 id")
    private Long brandId;

    @Schema(description = "状态 0草稿 1上架 2下架")
    private GoodsStatus status;
}
