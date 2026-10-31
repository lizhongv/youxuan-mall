package com.platform.cloud.mall.member.support.sms;

import com.platform.cloud.bootstrap.common.exception.BusinessException;

import java.io.Serial;

/**
 * 短信发送失败。继承 BusinessException：后台「测试发送」时能把渠道返回的错误码直接展示给管理员；
 * 验证码场景由 SmsCodeServiceImpl 捕获后换成对用户友好的提示。消息里只放错误码与渠道说明，不含密钥。
 */
public class SmsSendException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SmsSendException(String msg) {
        super(msg);
    }
}
