package com.platform.cloud.mall.trade.order.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 订单日志 VO */
@Data
public class OrderLogVO {

    private Long id;
    private String fromStatus;
    private String toStatus;
    private String operator;
    private String remark;
    private LocalDateTime createdAt;
}
