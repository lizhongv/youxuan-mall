package com.platform.cloud.bootstrap.goods.spu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "SKU 视图")
public class SkuVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long goodsId;
    private String skuNo;
    private String specs;
    private BigDecimal price;
    private Integer stock;
    private String image;
    private Integer enabled;
}
