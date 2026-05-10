package com.platform.cloud.bootstrap.common.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "分页查询基础参数")
public class PageQuery {

    @Min(value = 1, message = "≥ 1")
    @Schema(description = "页码（从 1 开始）", defaultValue = "1")
    private long page = 1;

    @Min(value = 1, message = "≥ 1")
    @Max(value = 200, message = "≤ 200")
    @Schema(description = "每页大小", defaultValue = "20")
    private long size = 20;

    @Schema(description = "搜索关键字")
    private String keyword;
}
