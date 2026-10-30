package com.platform.cloud.mall.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 账户流水（余额 / 积分 / 成长值），只增不改 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mbr_account_log")
public class MbrAccountLog extends BaseEntity {

    private Long memberId;
    private String accountType;
    private BigDecimal changeValue;
    private BigDecimal afterValue;
    private String bizType;
    private String bizNo;
    private String remark;
}
