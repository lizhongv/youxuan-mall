package com.platform.cloud.mall.goods.vo;

import com.platform.cloud.mall.goods.entity.SpecItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端商品详情，即编辑表单的回显结构，另带汇总后的价格、库存、销量。
 */
@Data
@Schema(description = "商品详情（管理端表单）")
public class GoodsDetailVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private String name;
    private String subtitle;
    private String pic;
    private List<String> album;
    private String detail;
    private String unit;
    private String keywords;
    private Integer virtualSales;
    private Integer sort;
    private String status;
    private Integer isNew;
    private Integer isHot;
    private Integer isRecommend;
    private List<SpecItem> specItems;
    private List<SkuVO> skus;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer sales;
    private LocalDateTime createdAt;
}
