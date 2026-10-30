package com.platform.cloud.mall.marketing.support;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 秒杀状态按 [startAt, endAt) 计算。
 */
class SeckillStateTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 11, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 11, 11, 12, 0);

    @Test
    void beforeStart() {
        assertThat(SeckillState.of(START, END, START.minusSeconds(1))).isEqualTo(SeckillState.NOT_STARTED);
    }

    @Test
    void exactlyStartIsOngoing() {
        assertThat(SeckillState.of(START, END, START)).isEqualTo(SeckillState.ONGOING);
    }

    @Test
    void middleIsOngoing() {
        assertThat(SeckillState.of(START, END, START.plusHours(1))).isEqualTo(SeckillState.ONGOING);
    }

    @Test
    void lastMomentBeforeEndIsOngoing() {
        assertThat(SeckillState.of(START, END, END.minusNanos(1))).isEqualTo(SeckillState.ONGOING);
    }

    @Test
    void exactlyEndIsEnded() {
        assertThat(SeckillState.of(START, END, END)).isEqualTo(SeckillState.ENDED);
    }

    @Test
    void afterEnd() {
        assertThat(SeckillState.of(START, END, END.plusDays(1))).isEqualTo(SeckillState.ENDED);
    }
}
