package com.platform.cloud.bootstrap.goods.spu.vo;

import com.platform.cloud.bootstrap.goods.spu.GoodsStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 列表视图（不带 SKU 列表，避免 N+1） */
@Data
@Schema(description = "商品摘要")
public class GoodsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String goodsNo;
    private String name;
    private Long categoryId;
    private Long brandId;
    private String coverImage;
    private String subtitle;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String unit;
    private GoodsStatus status;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
