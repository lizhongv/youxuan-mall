package com.platform.cloud.mall.trade.aftersale.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 售后分页筛选（会员端只用 status；管理端可用全部字段） */
@Data
@EqualsAndHashCode(callSuper = true)
public class AftersalePageQuery extends PageQuery {

    @Schema(description = "售后单号（管理端）")
    private String aftersaleNo;

    @Schema(description = "订单号（管理端）")
    private String orderNo;

    @Schema(description = "售后状态")
    private String status;

    @Schema(description = "售后类型 REFUND / RETURN_REFUND（管理端）")
    private String type;
}
