package com.platform.cloud.mall.member.service.impl;

import com.platform.cloud.mall.common.util.MaskUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 默认昵称、手机号脱敏、订单角标补零。
 */
class MemberMiscTest {

    @Test
    void defaultNicknameUsesLastFourDigits() {
        assertThat(MemberAuthServiceImpl.defaultNickname("13812345678")).isEqualTo("用户5678");
    }

    @Test
    void mobileMasking() {
        assertThat(MaskUtils.mobile("13812345678")).isEqualTo("138****5678");
        assertThat(MaskUtils.mobile("12345")).isEqualTo("1***5");
        assertThat(MaskUtils.mobile(null)).isNull();
        assertThat(MaskUtils.mobile("")).isEmpty();
    }

    @Test
    void orderCountsAlwaysContainAllKeys() {
        Map<String, Integer> counts = MemberProfileServiceImpl.normalizeOrderCounts(Map.of("PENDING_PAY", 2L));
        assertThat(counts).containsExactly(
                Map.entry("PENDING_PAY", 2),
                Map.entry("PENDING_DELIVERY", 0),
                Map.entry("DELIVERED", 0),
                Map.entry("TO_REVIEW", 0),
                Map.entry("AFTERSALE", 0));
        assertThat(MemberProfileServiceImpl.normalizeOrderCounts(null)).hasSize(5);
    }
}
