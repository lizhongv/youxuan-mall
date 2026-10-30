package com.platform.cloud.mall.marketing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 优惠券领取记录（管理端）。
 */
@Data
@Schema(description = "领取记录")
public class CouponRecordVO {

    private Long id;
    private Long memberId;
    @Schema(description = "会员手机号（脱敏）")
    private String memberMobile;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime usedAt;
    private Long orderId;
    private LocalDateTime createdAt;
}
