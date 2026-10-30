package com.platform.cloud.mall.member.support;

/**
 * 验证码使用场景。不同场景的验证码互不通用，防止用登录验证码去重置密码。
 */
public enum SmsScene {
    REGISTER,
    LOGIN,
    RESET
}
