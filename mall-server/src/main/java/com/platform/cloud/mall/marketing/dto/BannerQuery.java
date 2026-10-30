package com.platform.cloud.mall.marketing.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 轮播分页筛选：keyword 匹配标题。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BannerQuery extends PageQuery {

    @Schema(description = "展示位置")
    private String position;

    @Schema(description = "状态 1/0")
    private Integer status;
}
