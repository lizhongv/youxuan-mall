package com.platform.cloud.bootstrap.goods.spu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/** 详情视图（带 SKU + 富文本 description） */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品详情")
public class GoodsDetailVO extends GoodsVO {

    @Schema(description = "富文本详情")
    private String description;

    @Schema(description = "SKU 列表")
    private List<SkuVO> skus;
}
