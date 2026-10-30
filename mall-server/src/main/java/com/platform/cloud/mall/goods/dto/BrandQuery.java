package com.platform.cloud.mall.goods.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 品牌分页筛选：keyword 匹配名称。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BrandQuery extends PageQuery {

    @Schema(description = "状态 1/0")
    private Integer status;
}
