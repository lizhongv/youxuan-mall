package com.platform.cloud.mall.member.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 会员等级 */
@Data
public class LevelVO {

    private Long id;
    private String name;
    private Integer minGrowth;
    private BigDecimal discount;
    private String icon;
    private Integer sort;
}
