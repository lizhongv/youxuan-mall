package com.platform.cloud.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会员领取的优惠券。有效期在领取时就算好写入，模板后续修改不影响已领的券。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_member_coupon")
public class MemberCoupon extends BaseEntity {

    private Long memberId;
    private Long couponId;
    /** 见 {@link com.platform.cloud.mall.marketing.support.MemberCouponStatus} */
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime usedAt;
    private Long orderId;
}
