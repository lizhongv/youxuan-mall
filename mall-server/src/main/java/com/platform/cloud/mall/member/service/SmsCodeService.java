package com.platform.cloud.mall.member.service;

import com.platform.cloud.mall.member.support.SmsScene;
import com.platform.cloud.mall.member.vo.SmsCodeVO;

/**
 * 短信验证码：6 位数字，5 分钟有效，同一手机号 60 秒内只能发一次，错误 5 次作废。
 */
public interface SmsCodeService {

    /** 发送验证码；注册场景要求未注册，重置密码场景要求已注册 */
    SmsCodeVO send(String mobile, SmsScene scene);

    /** 校验并消费验证码；校验通过后立即失效，不能重复使用 */
    void verify(String mobile, SmsScene scene, String code);
}
