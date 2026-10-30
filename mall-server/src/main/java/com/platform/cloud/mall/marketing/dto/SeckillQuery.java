package com.platform.cloud.mall.marketing.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 秒杀活动分页筛选：keyword 匹配标题。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SeckillQuery extends PageQuery {

    @Schema(description = "NOT_STARTED / ONGOING / ENDED")
    private String state;

    @Schema(description = "状态 1/0")
    private Integer status;

    @Schema(description = "商品 ID")
    private Long goodsId;
}
