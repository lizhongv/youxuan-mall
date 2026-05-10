package com.platform.cloud.bootstrap.goods.spu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建商品（SPU + 至少 1 个 SKU）")
public class GoodsCreateRequest {

    @NotBlank @Size(max = 64)
    @Schema(description = "商品编号 SPU", example = "SPU-001")
    private String goodsNo;

    @NotBlank @Size(max = 120)
    @Schema(description = "商品名", example = "iPhone 17 Pro")
    private String name;

    @NotNull
    @Schema(description = "分类 id")
    private Long categoryId;

    @Schema(description = "品牌 id（可空）")
    private Long brandId;

    @Size(max = 255)
    private String coverImage;

    @Size(max = 255)
    private String subtitle;

    @Size(max = 65535)
    private String description;

    @Size(max = 16)
    private String unit;

    @Schema(description = "排序", defaultValue = "100")
    private Integer sortOrder = 100;

    @NotEmpty(message = "至少 1 个 SKU")
    @Valid
    private List<SkuUpsertRequest> skus;
}
