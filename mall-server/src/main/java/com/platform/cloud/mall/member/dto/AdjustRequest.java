package com.platform.cloud.mall.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** 后台调账：amount 正数增加、负数扣减；积分必须是整数 */
@Data
public class AdjustRequest {

    @NotBlank
    @Pattern(regexp = "^(BALANCE|POINTS)$", message = "账户类型只能是 BALANCE / POINTS")
    private String accountType;

    @NotNull
    private BigDecimal amount;

    @Size(max = 200)
    private String remark;
}
