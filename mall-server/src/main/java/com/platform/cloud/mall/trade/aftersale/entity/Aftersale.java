package com.platform.cloud.mall.trade.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 售后单（trd_aftersale）。状态取值与流转见 AftersaleStatus。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "trd_aftersale", autoResultMap = true)
public class Aftersale extends BaseEntity {

    private String aftersaleNo;
    private Long orderId;
    private Long orderItemId;
    private Long memberId;
    /** REFUND 仅退款 / RETURN_REFUND 退货退款 */
    private String type;
    private String status;
    private String reason;
    private String description;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;
    private BigDecimal amount;
    private Integer quantity;
    private String auditRemark;
    private LocalDateTime auditTime;
    private String returnCompany;
    private String returnNo;
    private LocalDateTime refundTime;
}
