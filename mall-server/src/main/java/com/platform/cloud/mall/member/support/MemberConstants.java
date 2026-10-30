package com.platform.cloud.mall.member.support;

/**
 * 会员领域约定常量。
 */
public final class MemberConstants {

    /** 令牌中的用户类型声明，refresh 时据此拒绝管理端令牌 */
    public static final String CLAIM_UTYPE = "utype";

    public static final int ENABLED = 1;
    public static final int FROZEN = 0;

    /** 默认昵称前缀，后接手机号后四位 */
    public static final String DEFAULT_NICKNAME_PREFIX = "用户";

    /** 流水业务类型 */
    public static final String BIZ_REGISTER_GIFT = "REGISTER_GIFT";
    public static final String BIZ_ADMIN_ADJUST = "ADMIN_ADJUST";

    private MemberConstants() {
    }
}
