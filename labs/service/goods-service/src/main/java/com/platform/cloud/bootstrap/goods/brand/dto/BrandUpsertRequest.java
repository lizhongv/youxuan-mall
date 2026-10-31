package com.platform.cloud.bootstrap.goods.brand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "品牌创建/更新请求")
public class BrandUpsertRequest {

    @NotBlank(message = "不能为空")
    @Size(max = 64)
    @Schema(description = "品牌名", example = "Apple")
    private String name;

    @Size(max = 255)
    private String logoUrl;

    @Size(max = 255)
    private String description;

    @Schema(description = "排序", defaultValue = "100")
    private Integer sortOrder = 100;

    @Schema(description = "是否可见 0否 1是", defaultValue = "1")
    private Integer visible = 1;
}
