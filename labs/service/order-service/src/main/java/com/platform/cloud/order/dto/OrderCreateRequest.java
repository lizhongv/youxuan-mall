package com.platform.cloud.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "创建订单请求")
public class OrderCreateRequest {

    @NotEmpty(message = "至少 1 项")
    @Valid
    private List<Item> items;

    @Size(max = 255)
    private String remark;

    @Data
    @Schema(description = "订单项")
    public static class Item {
        @NotNull
        private Long goodsId;
        @NotNull
        private Long skuId;
        @Size(max = 120)
        private String goodsName;
        @Size(max = 512)
        private String skuSpecs;
        @NotNull
        @DecimalMin(value = "0.01", message = "≥ 0.01")
        private BigDecimal unitPrice;
        @NotNull
        @Min(value = 1, message = "≥ 1")
        private Integer quantity;
    }
}
