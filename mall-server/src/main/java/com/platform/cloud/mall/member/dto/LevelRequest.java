package com.platform.cloud.mall.member.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** 新增 / 修改会员等级 */
@Data
public class LevelRequest {

    @NotBlank
    @Size(max = 32)
    private String name;

    @NotNull
    @Min(0)
    private Integer minGrowth;

    /** 0.10 ~ 1.00；低于 1 折基本是配置错误，直接拦住 */
    @NotNull
    @DecimalMin(value = "0.10", message = "折扣不能低于 0.10")
    @DecimalMax(value = "1.00", message = "折扣不能高于 1.00")
    @Digits(integer = 1, fraction = 2)
    private BigDecimal discount;

    @Size(max = 255)
    private String icon;

    private Integer sort;
}
