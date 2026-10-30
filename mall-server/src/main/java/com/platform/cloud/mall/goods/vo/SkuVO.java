package com.platform.cloud.mall.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * SKU。
 */
@Data
@Schema(description = "SKU")
public class SkuVO {

    private Long id;
    private List<String> specValues;
    @Schema(description = "展示用规格文本，如 颜色:红;尺码:XL")
    private String specText;
    private String code;
    private String pic;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer sales;
}
