package com.platform.cloud.goods.mq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * order_event:created 事件体（goods-service 侧的镜像 DTO）。
 * Phase 后期可抽到共享 platform-api 模块避免双份维护。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCreatedDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private List<Item> items;
    private LocalDateTime createdAt;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
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
