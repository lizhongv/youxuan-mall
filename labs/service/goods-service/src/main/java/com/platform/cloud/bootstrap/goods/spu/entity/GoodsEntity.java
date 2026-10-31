package com.platform.cloud.bootstrap.goods.spu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import com.platform.cloud.bootstrap.goods.spu.GoodsStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("goods")
public class GoodsEntity extends BaseEntity {

    private String goodsNo;
    private String name;
    private Long categoryId;
    private Long brandId;
    private String coverImage;
    private String subtitle;
    private String description;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String unit;
    private GoodsStatus status;
    private Integer sortOrder;
}
