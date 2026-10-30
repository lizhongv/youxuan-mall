package com.platform.cloud.mall.trade.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 订单状态流转日志（trd_order_log），客服排查纠纷的依据，只增不改。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trd_order_log")
public class OrderLog extends BaseEntity {

    private Long orderId;
    private String fromStatus;
    private String toStatus;
    /** member:id / admin:id / system */
    private String operator;
    private String remark;
}
