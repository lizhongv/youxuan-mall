package com.platform.cloud.mall.goods.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商城端商品搜索：keyword 匹配名称 / 副标题 / 关键词。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppGoodsQuery extends PageQuery {

    @Schema(description = "类目 ID，一级或二级；一级时包含其全部子类目")
    private Long categoryId;

    @Schema(description = "品牌 ID")
    private Long brandId;

    @Pattern(regexp = "^(DEFAULT|SALES|PRICE_ASC|PRICE_DESC|NEW)?$", message = "不支持的排序方式")
    @Schema(description = "DEFAULT / SALES / PRICE_ASC / PRICE_DESC / NEW", defaultValue = "DEFAULT")
    private String sort;
}
