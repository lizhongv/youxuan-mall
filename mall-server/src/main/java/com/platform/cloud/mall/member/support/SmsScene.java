package com.platform.cloud.mall.member.support;

/**
 * 验证码使用场景。不同场景的验证码互不通用，防止用登录验证码去重置密码。
 */
public enum SmsScene {
    REGISTER,
    LOGIN,
    RESET,
    /**
     * 设置 / 修改余额支付密码。只能由已登录会员通过 POST /app/member/pay-password/sms-code 发给自己的手机号，
     * 公开的 /app/auth/sms-code 不接受该场景：否则任何人都能给任意手机号发「设置支付密码」短信。
     */
    PAY_PASSWORD
}
