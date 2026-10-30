package com.platform.cloud.mall.member.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收藏 / 足迹里的商品项。id 为商品 ID，便于前端直接跳转详情；商品被删除时 name 等为空、onSale 为 false。
 */
@Data
public class MemberGoodsVO {

    private Long id;
    private String name;
    private String pic;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer sales;
    private Boolean onSale;
    /** 收藏时间（收藏列表） */
    private LocalDateTime favoritedAt;
    /** 最近浏览时间（足迹列表） */
    private LocalDateTime browsedAt;
}
