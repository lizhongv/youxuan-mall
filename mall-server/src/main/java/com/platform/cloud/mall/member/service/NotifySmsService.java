package com.platform.cloud.mall.member.service;

import com.platform.cloud.mall.member.support.sms.SmsProvider;
import com.platform.cloud.mall.member.support.sms.SmsTemplate;

import java.util.LinkedHashMap;

/**
 * 通知短信（支付成功、发货、退款成功等），供交易等领域在业务事件后调用。
 */
public interface NotifySmsService {

    /**
     * 发送通知短信。通知是「锦上添花」，任何失败（未配置模板、渠道报错）都只记日志、返回 false，
     * 绝不抛异常影响调用方业务；建议在事务提交后调用（如 TxHooks.afterCommit），避免回滚了还发出短信。
     *
     * @param params 有序参数，顺序需与腾讯云模板变量 {1}{2}… 一致
     * @return 是否已交给渠道发送
     */
    boolean send(String mobile, SmsTemplate template, LinkedHashMap<String, String> params);

    /**
     * 后台「测试发送」：用验证码模板给指定手机号发一条随机验证码，失败直接抛出渠道错误码便于排查。
     *
     * @return 实际使用的渠道
     */
    SmsProvider sendTest(String mobile);
}
