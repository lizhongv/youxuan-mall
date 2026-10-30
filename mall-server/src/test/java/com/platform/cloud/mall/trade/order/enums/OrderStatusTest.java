package com.platform.cloud.mall.trade.order.enums;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.trade.aftersale.enums.AftersaleStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusTest {

    @ParameterizedTest(name = "{0} → {1} 合法")
    @CsvSource({
            "PENDING_PAY, PENDING_DELIVERY",
            "PENDING_PAY, CANCELLED",
            "PENDING_DELIVERY, DELIVERED",
            "PENDING_DELIVERY, CANCELLED",
            "DELIVERED, COMPLETED"
    })
    void 合法流转(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} → {1} 非法")
    @CsvSource({
            "PENDING_PAY, DELIVERED",
            "PENDING_PAY, COMPLETED",
            "PENDING_PAY, PENDING_PAY",
            "PENDING_DELIVERY, PENDING_PAY",
            "PENDING_DELIVERY, COMPLETED",
            "DELIVERED, CANCELLED",
            "DELIVERED, PENDING_DELIVERY",
            "COMPLETED, CANCELLED",
            "COMPLETED, DELIVERED",
            "CANCELLED, PENDING_PAY",
            "CANCELLED, PENDING_DELIVERY"
    })
    void 非法流转(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitTo(to)).isFalse();
    }

    @Test
    void 终态没有后续状态() {
        assertThat(OrderStatus.COMPLETED.nextStatuses()).isEmpty();
        assertThat(OrderStatus.CANCELLED.nextStatuses()).isEmpty();
        assertThat(OrderStatus.PENDING_PAY.canTransitTo(null)).isFalse();
    }

    @Test
    void 未知状态值报错_展示时原样返回() {
        assertThatThrownBy(() -> OrderStatus.of("PAID")).isInstanceOf(BusinessException.class);
        assertThat(OrderStatus.textOf("PAID")).isEqualTo("PAID");
        assertThat(OrderStatus.textOf("DELIVERED")).isEqualTo("待收货");
    }

    @ParameterizedTest(name = "售后 {0} → {1} = {2}")
    @CsvSource({
            "APPLYING, REFUNDED, true",
            "APPLYING, WAIT_RETURN, true",
            "APPLYING, REJECTED, true",
            "APPLYING, CANCELLED, true",
            "APPLYING, RETURNING, false",
            "WAIT_RETURN, RETURNING, true",
            "WAIT_RETURN, CANCELLED, true",
            "WAIT_RETURN, REFUNDED, false",
            "WAIT_RETURN, REJECTED, false",
            "RETURNING, REFUNDED, true",
            "RETURNING, CANCELLED, false",
            "RETURNING, REJECTED, false",
            "REFUNDED, APPLYING, false",
            "REJECTED, APPLYING, false",
            "CANCELLED, APPLYING, false"
    })
    void 售后状态流转(AftersaleStatus from, AftersaleStatus to, boolean allowed) {
        assertThat(from.canTransitTo(to)).isEqualTo(allowed);
    }
}
