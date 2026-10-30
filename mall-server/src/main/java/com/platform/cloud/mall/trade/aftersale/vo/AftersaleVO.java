package com.platform.cloud.mall.trade.aftersale.vo;

import com.platform.cloud.mall.trade.order.vo.OrderItemVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 售后 VO（item 为订单明细快照） */
@Data
public class AftersaleVO {

    private Long id;
    private String aftersaleNo;
    private Long orderId;
    private String orderNo;
    private Long orderItemId;
    private Long memberId;
    private String memberNickname;
    private String type;
    private String status;
    private String statusText;
    private String reason;
    private String description;
    private List<String> images;
    private BigDecimal amount;
    private Integer quantity;
    private String auditRemark;
    private LocalDateTime auditTime;
    private String returnCompany;
    private String returnNo;
    private LocalDateTime refundTime;
    private LocalDateTime createdAt;
    private OrderItemVO item;
}
