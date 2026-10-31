package com.platform.cloud.mall.goods.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品 SPU。price / originalPrice / stock 是从 SKU 汇总的冗余字段，只在保存商品和扣减库存时维护，
 * 目的是让列表页不用再关联 SKU 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gds_goods", autoResultMap = true)
public class Goods extends BaseEntity {

    private Long categoryId;
    /** 品牌可清空，因此更新时 null 也要写入 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long brandId;
    /** 运费模板；空表示用默认模板，可清空，因此更新时 null 也写入 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long freightTemplateId;
    private String name;
    private String subtitle;
    private String pic;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> album;
    private String detail;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<SpecItem> specItems;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer sales;
    private Integer virtualSales;
    private String unit;
    private String keywords;
    /** 见 {@link com.platform.cloud.mall.goods.support.GoodsStatus} */
    private String status;
    private Integer isNew;
    private Integer isHot;
    private Integer isRecommend;
    private Integer sort;
}
