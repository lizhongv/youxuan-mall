package com.platform.cloud.mall.trade.order.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** 管理端订单分页筛选 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminOrderPageQuery extends PageQuery {

    @Schema(description = "订单号（精确）")
    private String orderNo;

    @Schema(description = "收货人手机号（精确）")
    private String receiverMobile;

    @Schema(description = "订单状态")
    private String status;

    @Schema(description = "订单类型 NORMAL / SECKILL")
    private String orderType;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "下单时间起")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "下单时间止")
    private LocalDateTime endTime;
}
