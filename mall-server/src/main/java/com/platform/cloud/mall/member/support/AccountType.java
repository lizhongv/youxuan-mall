package com.platform.cloud.mall.member.support;

/**
 * 账户类型，对应 mbr_account_log.account_type。
 */
public enum AccountType {
    /** 余额（元） */
    BALANCE,
    /** 积分，可抵扣、可扣减 */
    POINTS,
    /** 成长值，只增不减，决定等级 */
    GROWTH
}
