package com.platform.cloud.bootstrap.goods.spu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "SKU 创建/更新请求")
public class SkuUpsertRequest {

    @NotBlank(message = "不能为空")
    @Size(max = 64)
    @Schema(description = "SKU 编号", example = "SKU-001-RED-M")
    private String skuNo;

    @Size(max = 512)
    @Schema(description = "规格 JSON", example = "{\"color\":\"red\",\"size\":\"M\"}")
    private String specs;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = true, message = "≥ 0")
    private BigDecimal price;

    @NotNull
    @Min(value = 0, message = "≥ 0")
    private Integer stock;

    @Size(max = 255)
    private String image;

    @Schema(description = "是否启用 0否 1是", defaultValue = "1")
    private Integer enabled = 1;
}
