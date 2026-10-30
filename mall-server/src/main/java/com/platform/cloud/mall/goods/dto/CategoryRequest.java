package com.platform.cloud.mall.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 类目新增 / 修改。
 */
@Data
@Schema(description = "类目表单")
public class CategoryRequest {

    @Schema(description = "父类目 ID，0 或空为一级类目")
    private Long parentId;

    @NotBlank(message = "类目名称不能为空")
    @Size(max = 64, message = "类目名称最多 64 个字")
    private String name;

    @Size(max = 255, message = "图标地址过长")
    private String icon;

    private Integer sort;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
