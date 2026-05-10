package com.platform.cloud.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class OrderEntity extends BaseEntity {

    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    /** 0待支付 1已支付 2已发货 3已完成 4已取消 */
    private Integer status;
    private String remark;
}
