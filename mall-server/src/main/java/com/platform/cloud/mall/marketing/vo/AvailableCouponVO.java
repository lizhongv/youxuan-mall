package com.platform.cloud.mall.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 领券中心的可领券。
 */
@Data
@Schema(description = "可领取的优惠券")
public class AvailableCouponVO {

    private Long id;
    private String name;
    private String type;
    private BigDecimal threshold;
    private BigDecimal amount;
    private BigDecimal discount;
    private BigDecimal maxAmount;
    private String scope;
    private String validType;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer validDays;
    private Integer perLimit;
    private String description;
    @Schema(description = "剩余可领数量，不限量时不返回")
    private Integer remaining;
    @Schema(description = "当前会员已领数量；未登录时不返回")
    private Integer received;
}
