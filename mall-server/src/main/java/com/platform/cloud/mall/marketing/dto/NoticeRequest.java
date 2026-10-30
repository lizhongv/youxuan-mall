package com.platform.cloud.mall.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公告新增 / 修改。
 */
@Data
@Schema(description = "公告表单")
public class NoticeRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题最多 128 个字")
    private String title;

    /** TEXT 列上限 64KB，按 UTF-8 中文 3 字节留余量 */
    @Size(max = 20000, message = "正文过长")
    private String content;

    private Integer sort;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
