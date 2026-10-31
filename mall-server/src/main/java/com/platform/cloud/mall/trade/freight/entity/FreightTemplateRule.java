package com.platform.cloud.mall.trade.freight.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/** 运费模板计费规则（trd_freight_rule）。provinces 为空数组表示「默认地区」。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "trd_freight_rule", autoResultMap = true)
public class FreightTemplateRule extends BaseEntity {

    private Long templateId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> provinces;
    private BigDecimal firstUnit;
    private BigDecimal firstFee;
    private BigDecimal nextUnit;
    private BigDecimal nextFee;
    private Integer sort;
}
