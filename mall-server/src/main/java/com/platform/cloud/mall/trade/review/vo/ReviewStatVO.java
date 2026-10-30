package com.platform.cloud.mall.trade.review.vo;

/** 评价分档统计；goodRate 为 4-5 星占比的整数百分比，无评价时为 100 */
public record ReviewStatVO(long total, long good, long medium, long bad, long image, int goodRate) {
}
