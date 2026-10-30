package com.platform.cloud.mall.marketing.support;

import java.time.LocalDateTime;

/**
 * 秒杀活动时间状态，由当前时间与起止时间计算，不落库（落库就得靠定时任务刷新，容易不准）。
 * 区间为左闭右开 [startAt, endAt)：开始那一刻即可抢，结束那一刻即停止。
 */
public enum SeckillState {
    NOT_STARTED,
    ONGOING,
    ENDED;

    public static SeckillState of(LocalDateTime startAt, LocalDateTime endAt, LocalDateTime now) {
        if (now.isBefore(startAt)) {
            return NOT_STARTED;
        }
        if (!now.isBefore(endAt)) {
            return ENDED;
        }
        return ONGOING;
    }
}
