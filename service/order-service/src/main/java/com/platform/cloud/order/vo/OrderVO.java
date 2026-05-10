package com.platform.cloud.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "订单视图")
public class OrderVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
