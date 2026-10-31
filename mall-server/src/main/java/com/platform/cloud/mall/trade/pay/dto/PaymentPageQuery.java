package com.platform.cloud.mall.trade.pay.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** 后台支付流水筛选 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentPageQuery extends PageQuery {

    @Schema(description = "支付单号（精确）")
    private String payNo;

    @Schema(description = "订单号（精确）")
    private String orderNo;

    @Schema(description = "渠道 BALANCE / WECHAT / ALIPAY / MOCK")
    private String channel;

    @Schema(description = "状态 WAITING / SUCCESS / CLOSED / REFUNDED / REFUNDED_AFTER_CLOSE")
    private String status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发起时间起")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发起时间止")
    private LocalDateTime endTime;
}
