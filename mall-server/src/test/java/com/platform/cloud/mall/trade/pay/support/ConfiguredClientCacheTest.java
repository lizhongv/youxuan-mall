package com.platform.cloud.mall.trade.pay.support;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SDK 客户端缓存：配置不变复用、配置变化重建、构建失败短时间内不重试。
 */
class ConfiguredClientCacheTest {

    @Test
    void rebuildsOnlyWhenSettingsChange() {
        AtomicInteger builds = new AtomicInteger();
        ConfiguredClientCache<String, String> cache = new ConfiguredClientCache<>("t",
                s -> "client-" + s + "-" + builds.incrementAndGet());

        assertThat(cache.get("a")).contains("client-a-1");
        assertThat(cache.get("a")).contains("client-a-1");
        assertThat(cache.get("b")).contains("client-b-2");
    }

    @Test
    void failureIsCachedForBackoffThenRetried() {
        AtomicInteger builds = new AtomicInteger();
        AtomicLong now = new AtomicLong(0);
        ConfiguredClientCache<String, String> cache = new ConfiguredClientCache<>("t", s -> {
            builds.incrementAndGet();
            throw new IllegalArgumentException("bad key");
        }, now::get);

        assertThat(cache.get("a")).isEmpty();
        assertThat(cache.get("a")).isEmpty();
        assertThat(builds.get()).isEqualTo(1);
        now.set(ConfiguredClientCache.FAILURE_BACKOFF_MILLIS + 1);
        assertThat(cache.get("a")).isEmpty();
        assertThat(builds.get()).isEqualTo(2);
    }

    @Test
    void amountsAreComparedInFen() {
        assertThat(PayAmounts.toFen(new java.math.BigDecimal("88.8"))).isEqualTo(8880);
        assertThat(PayAmounts.yuanTextToFen("88.80")).isEqualTo(8880);
        assertThat(PayAmounts.yuanTextToFen("abc")).isEqualTo(-1);
        assertThat(PayAmounts.toYuanText(new java.math.BigDecimal("1"))).isEqualTo("1.00");
    }
}
