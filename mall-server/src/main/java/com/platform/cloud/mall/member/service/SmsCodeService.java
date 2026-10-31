package com.platform.cloud.mall.member.service;

import com.platform.cloud.mall.member.support.SmsScene;
import com.platform.cloud.mall.member.vo.SmsCodeVO;

/**
 * 短信验证码：6 位数字，5 分钟有效，同一手机号 60 秒内只能发一次，错误 5 次作废。
 */
public interface SmsCodeService {

    /**
     * 发送验证码。必须先通过图形验证码，并受「手机号 60 秒 / 每天、IP 每小时、全站每分钟」四层频控。
     * <p>
     * 不论手机号是否已注册都发送并返回成功：注册 / 重置密码场景若在这里提示「已注册 / 未注册」，
     * 接口就成了手机号是否为本站会员的查询工具。是否已注册在提交注册 / 重置密码时再提示（只有收到验证码的本人能看到）。
     */
    SmsCodeVO send(String mobile, SmsScene scene, String captchaId, String captchaCode);

    /** 校验并消费验证码；校验通过后立即失效，不能重复使用 */
    void verify(String mobile, SmsScene scene, String code);
}
