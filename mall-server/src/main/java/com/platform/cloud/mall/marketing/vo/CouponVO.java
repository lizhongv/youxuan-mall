package com.platform.cloud.mall.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券模板（管理端），字段同 mkt_coupon。
 */
@Data
@Schema(description = "优惠券模板")
public class CouponVO {

    private Long id;
    private String name;
    private String type;
    private BigDecimal threshold;
    private BigDecimal amount;
    private BigDecimal discount;
    private BigDecimal maxAmount;
    private String scope;
    private List<Long> scopeIds;
    private Integer total;
    private Integer issued;
    private Integer used;
    private Integer perLimit;
    private String validType;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer validDays;
    private Integer status;
    private String description;
    private LocalDateTime createdAt;
}
