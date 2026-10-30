package com.platform.cloud.mall.trade.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 订单 VO（管理端与商城端共用；logs、member 只在详情返回） */
@Data
@Schema(description = "订单")
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long memberId;
    private String memberNickname;
    private String orderType;
    private String status;
    private String statusText;
    private BigDecimal goodsAmount;
    private BigDecimal freightAmount;
    private BigDecimal couponAmount;
    private BigDecimal discountAmount;
    private BigDecimal adjustAmount;
    private BigDecimal payAmount;
    private String payType;
    private LocalDateTime payTime;
    private LocalDateTime payExpireAt;
    private String receiverName;
    private String receiverMobile;
    private String receiverProvince;
    private String receiverCity;
    private String receiverDistrict;
    private String receiverDetail;
    private String remark;
    private String adminRemark;
    private String deliveryCompany;
    private String deliveryNo;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private Integer reviewed;
    private LocalDateTime createdAt;
    private List<OrderItemVO> items;
    @Schema(description = "状态日志（详情）")
    private List<OrderLogVO> logs;
    @Schema(description = "买家信息（管理端详情）")
    private MemberInfo member;

    @Data
    public static class MemberInfo {
        private Long id;
        private String nickname;
        private String mobile;
    }
}
