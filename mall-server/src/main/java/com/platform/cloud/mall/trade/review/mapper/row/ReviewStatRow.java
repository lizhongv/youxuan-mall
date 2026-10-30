package com.platform.cloud.mall.trade.review.mapper.row;

import lombok.Data;

/** 评价分档计数 */
@Data
public class ReviewStatRow {
    private Long total;
    private Long good;
    private Long medium;
    private Long bad;
    private Long image;
}
