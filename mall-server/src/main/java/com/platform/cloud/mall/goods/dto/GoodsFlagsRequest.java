package com.platform.cloud.mall.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 设置新品 / 热卖 / 推荐标签；为空的字段保持不变，方便列表上单独切换某一个开关。
 */
@Data
@Schema(description = "商品标签")
public class GoodsFlagsRequest {

    @Min(0) @Max(1)
    private Integer isNew;

    @Min(0) @Max(1)
    private Integer isHot;

    @Min(0) @Max(1)
    private Integer isRecommend;
}
