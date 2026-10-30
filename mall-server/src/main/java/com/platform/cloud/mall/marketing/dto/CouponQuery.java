package com.platform.cloud.mall.marketing.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 优惠券分页筛选：keyword 匹配名称。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CouponQuery extends PageQuery {

    @Schema(description = "FULL_REDUCTION / DISCOUNT / NO_THRESHOLD")
    private String type;

    @Schema(description = "状态 1/0")
    private Integer status;
}
