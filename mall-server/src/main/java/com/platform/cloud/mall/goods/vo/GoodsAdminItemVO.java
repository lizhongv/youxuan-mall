package com.platform.cloud.mall.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端商品列表项。
 */
@Data
@Schema(description = "商品列表项（管理端）")
public class GoodsAdminItemVO {

    private Long id;
    private String name;
    private String pic;
    private Long categoryId;
    private String categoryName;
    private String brandName;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer sales;
    private String status;
    private Integer isNew;
    private Integer isHot;
    private Integer isRecommend;
    private Integer sort;
    private LocalDateTime createdAt;
}
