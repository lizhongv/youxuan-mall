package com.platform.cloud.bootstrap.goods.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "分类创建/更新请求")
public class CategoryUpsertRequest {

    @Schema(description = "父分类 id，0 表示根", defaultValue = "0")
    @Min(value = 0, message = "≥ 0")
    private Long parentId = 0L;

    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "长度 ≤ 64")
    @Schema(description = "分类名", example = "数码")
    private String name;

    @Size(max = 64)
    private String slug;

    @Size(max = 255)
    private String iconUrl;

    @Size(max = 255)
    private String bannerUrl;

    @Schema(description = "排序，升序", defaultValue = "100")
    private Integer sortOrder = 100;

    @Schema(description = "是否可见 0否 1是", defaultValue = "1")
    private Integer visible = 1;
}
