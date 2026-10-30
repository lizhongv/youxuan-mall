package com.platform.cloud.mall.goods.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 品牌新增 / 修改。
 */
@Data
@Schema(description = "品牌表单")
public class BrandRequest {

    @NotBlank(message = "品牌名称不能为空")
    @Size(max = 64, message = "品牌名称最多 64 个字")
    private String name;

    @Size(max = 255, message = "Logo 地址过长")
    private String logo;

    @Pattern(regexp = "^[A-Za-z]?$", message = "首字母只能是一个英文字母")
    private String firstLetter;

    @Size(max = 500, message = "品牌介绍最多 500 个字")
    private String description;

    private Integer sort;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
