package com.platform.cloud.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动（单 SKU）。sold 是数据库侧的最终防线，用条件更新维护；Redis 只做前置挡流。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_seckill")
public class Seckill extends BaseEntity {

    private String title;
    private Long goodsId;
    private Long skuId;
    private BigDecimal seckillPrice;
    private Integer stock;
    private Integer sold;
    private Integer limitPerMember;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer status;
}
