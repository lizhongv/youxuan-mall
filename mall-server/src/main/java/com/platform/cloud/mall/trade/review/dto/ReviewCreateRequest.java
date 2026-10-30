package com.platform.cloud.mall.trade.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 发表评价 */
@Data
public class ReviewCreateRequest {

    @NotNull(message = "请选择评价商品")
    private Long orderItemId;

    @NotNull(message = "请打分")
    @Min(value = 1, message = "评分 1-5 星")
    @Max(value = 5, message = "评分 1-5 星")
    private Integer rating;

    @Size(max = 1000, message = "评价内容不能超过 1000 字")
    private String content;

    @Size(max = 9, message = "最多上传 9 张图片")
    private List<@NotBlank @Size(max = 255) String> images;

    @Schema(description = "是否匿名")
    private Boolean anonymous;
}
