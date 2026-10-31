package com.platform.cloud.mall.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 我的优惠券。id 为会员券 ID（下单时传 memberCouponId），couponId 为模板 ID。
 */
@Data
@Schema(description = "我的优惠券")
public class MyCouponVO {

    private Long id;
    private Long couponId;
    private String name;
    private String type;
    private BigDecimal threshold;
    private BigDecimal amount;
    private BigDecimal discount;
    private BigDecimal maxAmount;
    private String scope;
    @Schema(description = "适用的类目或商品 ID（scope 为 CATEGORY / GOODS 时），前端「去使用」据此跳转商品列表")
    private List<Long> scopeIds;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    @Schema(description = "UNUSED / USED / EXPIRED；已过期但定时任务未处理的券也显示为 EXPIRED")
    private String status;
    private String description;
}
