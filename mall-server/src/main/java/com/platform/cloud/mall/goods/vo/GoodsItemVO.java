package com.platform.cloud.mall.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商城端商品列表项。sales 为展示销量（真实销量 + 虚拟销量）。会进入首页缓存，保持普通 JavaBean。
 */
@Data
@Schema(description = "商品列表项（商城端）")
public class GoodsItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String subtitle;
    private String pic;
    private BigDecimal price;
    private BigDecimal originalPrice;
    @Schema(description = "展示销量 = 真实销量 + 虚拟销量")
    private Integer sales;
    private Integer isNew;
    private Integer isHot;
}
