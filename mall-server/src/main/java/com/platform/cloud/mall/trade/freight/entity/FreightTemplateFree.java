package com.platform.cloud.mall.trade.freight.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/** 运费模板指定条件包邮（trd_freight_free）：指定地区满 N 件或满 X 元包邮，provinces 为空表示全部地区。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "trd_freight_free", autoResultMap = true)
public class FreightTemplateFree extends BaseEntity {

    private Long templateId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> provinces;
    private Integer minCount;
    private BigDecimal minAmount;
}
