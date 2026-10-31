package com.platform.cloud.mall.trade.aftersale.vo;

import com.platform.cloud.mall.trade.order.vo.OrderItemVO;
import com.platform.cloud.mall.trade.common.TradeSettings.ReturnAddress;
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
    /**
     * 退货收件信息，仅退货退款且处于 WAIT_RETURN / RETURNING / REFUNDING / REFUNDED 时返回，其余为 null：
     * 审核通过前不暴露商家地址，三项都未配置时也为 null。取当前系统参数，不做快照（地址变更后买家应寄往新地址）。
     */
    private ReturnAddress returnAddress;
}
