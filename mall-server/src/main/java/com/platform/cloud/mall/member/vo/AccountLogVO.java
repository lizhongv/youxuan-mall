package com.platform.cloud.mall.member.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 账户流水 */
@Data
public class AccountLogVO {

    private Long id;
    private Long memberId;
    private String accountType;
    private BigDecimal changeValue;
    private BigDecimal afterValue;
    private String bizType;
    private String bizNo;
    private String remark;
    private LocalDateTime createdAt;
}
