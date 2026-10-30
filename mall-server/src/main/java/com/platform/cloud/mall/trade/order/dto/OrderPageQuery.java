package com.platform.cloud.mall.trade.order.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 我的订单分页查询 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderPageQuery extends PageQuery {

    /** 待评价：已完成且未全部评价 */
    public static final String TO_REVIEW = "TO_REVIEW";

    @Schema(description = "订单状态；额外支持 TO_REVIEW（已完成未评价）；空为全部")
    private String status;
}
