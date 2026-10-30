package com.platform.cloud.mall.trade.order.mapper.row;

import lombok.Data;

/** 统计查询结果：按状态计数 */
@Data
public class StatusCountRow {
    private String status;
    private Long cnt;
}
