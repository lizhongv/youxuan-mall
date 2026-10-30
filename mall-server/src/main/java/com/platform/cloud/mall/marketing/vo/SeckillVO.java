package com.platform.cloud.mall.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动。state 由当前时间与活动起止时间计算，不落库。
 */
@Data
@Schema(description = "秒杀活动")
public class SeckillVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private Long goodsId;
    private String goodsName;
    private String goodsPic;
    private Long skuId;
    private String specText;
    @Schema(description = "SKU 当前售价（划线对比用）")
    private BigDecimal price;
    private BigDecimal seckillPrice;
    @Schema(description = "活动库存")
    private Integer stock;
    @Schema(description = "已抢数量")
    private Integer sold;
    private Integer limitPerMember;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer status;
    @Schema(description = "NOT_STARTED / ONGOING / ENDED")
    private String state;
}
