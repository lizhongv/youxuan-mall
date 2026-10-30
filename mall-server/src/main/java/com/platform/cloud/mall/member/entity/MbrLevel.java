package com.platform.cloud.mall.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 会员等级 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mbr_level")
public class MbrLevel extends BaseEntity {

    private String name;
    private Integer minGrowth;
    private BigDecimal discount;
    private String icon;
    private Integer sort;
}
