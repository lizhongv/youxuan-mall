package com.platform.cloud.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券模板。issued / used 只通过条件更新累加，防止并发超发。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "mkt_coupon", autoResultMap = true)
public class Coupon extends BaseEntity {

    private String name;
    /** 见 {@link com.platform.cloud.mall.marketing.support.CouponType} */
    private String type;
    private BigDecimal threshold;
    private BigDecimal amount;
    private BigDecimal discount;
    private BigDecimal maxAmount;
    /** 见 {@link com.platform.cloud.mall.marketing.support.CouponScope} */
    private String scope;
    /** 范围为全场时清空，因此更新时 null 也写入 */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<Long> scopeIds;
    private Integer total;
    private Integer issued;
    private Integer used;
    private Integer perLimit;
    private String validType;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime startAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime endAt;
    private Integer validDays;
    private Integer status;
    private String description;
}
