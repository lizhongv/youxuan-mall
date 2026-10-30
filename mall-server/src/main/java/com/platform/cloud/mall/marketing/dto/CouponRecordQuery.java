package com.platform.cloud.mall.marketing.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 领取记录分页筛选。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CouponRecordQuery extends PageQuery {

    @Schema(description = "UNUSED / USED / EXPIRED")
    private String status;

    @Schema(description = "会员 ID")
    private Long memberId;
}
