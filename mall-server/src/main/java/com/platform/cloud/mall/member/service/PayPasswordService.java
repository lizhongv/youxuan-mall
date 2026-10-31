package com.platform.cloud.mall.member.service;

import com.platform.cloud.mall.member.dto.PayPasswordRequest;
import com.platform.cloud.mall.member.vo.SmsCodeVO;

/**
 * 余额支付密码：6 位数字，BCrypt 存储；连续错误 5 次锁定 30 分钟。
 */
public interface PayPasswordService {

    /** 给当前会员自己的手机号发送 PAY_PASSWORD 场景验证码（同样受图形验证码与短信频控约束） */
    SmsCodeVO sendSmsCode(Long memberId, String captchaId, String captchaCode);

    /** 凭短信验证码设置或修改支付密码；成功后清零错误计数（解除锁定） */
    void setPayPassword(Long memberId, PayPasswordRequest req);

    /**
     * 校验支付密码，失败抛业务异常：60010 未设置、60011 错误（含剩余次数）、60012 已锁定。
     * 锁定期间不校验密码直接拒绝，避免锁定期内仍可借响应差异继续试密码。
     */
    void verify(Long memberId, String payPassword);
}
