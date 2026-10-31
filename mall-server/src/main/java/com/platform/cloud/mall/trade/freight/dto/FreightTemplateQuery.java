package com.platform.cloud.mall.trade.freight.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 运费模板分页筛选：keyword 匹配名称。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FreightTemplateQuery extends PageQuery {

    @Schema(description = "BY_COUNT / BY_WEIGHT")
    private String chargeType;

    @Schema(description = "状态 1/0")
    private Integer status;
}
