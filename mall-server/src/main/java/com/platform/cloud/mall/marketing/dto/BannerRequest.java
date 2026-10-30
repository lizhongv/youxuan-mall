package com.platform.cloud.mall.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 轮播新增 / 修改。
 */
@Data
@Schema(description = "轮播表单")
public class BannerRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 64, message = "标题最多 64 个字")
    private String title;

    @NotBlank(message = "请上传图片")
    @Size(max = 255, message = "图片地址过长")
    private String image;

    @Pattern(regexp = "^(NONE|GOODS|CATEGORY|URL)?$", message = "跳转类型只能是 NONE / GOODS / CATEGORY / URL")
    private String linkType;

    @Size(max = 255, message = "跳转值过长")
    private String linkValue;

    @Pattern(regexp = "^[A-Z_]{0,16}$", message = "位置编码只能是大写字母")
    @Schema(description = "展示位置，默认 HOME")
    private String position;

    private Integer sort;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
