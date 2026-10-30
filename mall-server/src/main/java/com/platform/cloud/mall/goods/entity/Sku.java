package com.platform.cloud.mall.goods.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品 SKU。specValues 与 SPU 的 specItems 顺序一一对应；specText 是下单快照与展示用的冗余文本。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gds_sku", autoResultMap = true)
public class Sku extends BaseEntity {

    private Long goodsId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> specValues;
    private String specText;
    private String code;
    private String pic;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer sales;
}
