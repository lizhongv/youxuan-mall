package com.platform.cloud.bootstrap.goods.spu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新商品 SPU 字段（不含 SKU；SKU 走 /skus 子资源）")
public class GoodsUpdateRequest {

    @Size(max = 120)
    private String name;

    private Long categoryId;
    private Long brandId;

    @Size(max = 255)
    private String coverImage;

    @Size(max = 255)
    private String subtitle;

    @Size(max = 65535)
    private String description;

    @Size(max = 16)
    private String unit;

    private Integer sortOrder;
}
