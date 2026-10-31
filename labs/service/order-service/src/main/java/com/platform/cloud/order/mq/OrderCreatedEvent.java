package com.platform.cloud.order.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * order_event:created 事件体。下游消费者（库存/优惠券/通知）按需消费。
 * <p>真实环境会抽到 platform-api-order 共享模块给消费者复用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private List<Item> items;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long goodsId;
        private Long skuId;
        private String skuSpecs;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
