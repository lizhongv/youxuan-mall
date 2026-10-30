package com.platform.cloud.mall.member.support;

/**
 * 短信发送渠道。默认实现只打日志；接入阿里云 / 腾讯云短信时提供一个该类型的 Bean 即可替换。
 */
public interface SmsSender {

    /**
     * 发送验证码。发送失败应抛异常，由调用方回滚本次验证码。
     */
    void sendCode(String mobile, SmsScene scene, String code);
}
