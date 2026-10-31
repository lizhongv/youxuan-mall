package com.platform.cloud.mall.member.support.sms;

import com.platform.cloud.mall.member.support.SmsScene;
import com.platform.cloud.mall.member.support.SmsSender;

import java.util.LinkedHashMap;

/**
 * 真实短信渠道：在验证码之外，还能按模板发送通知短信。
 * <p>
 * 参数用有序 Map：阿里云按变量名（JSON）填充，腾讯云按位置（{1}{2}…）填充，
 * 同一份参数在两家都能用，前提是 Map 的插入顺序与腾讯云模板变量顺序一致。
 */
public interface SmsChannel extends SmsSender {

    /** 验证码模板的变量名。阿里云模板写 ${code}；腾讯云模板只能有一个变量 {1} */
    String CODE_PARAM = "code";

    /**
     * 按模板发送。
     *
     * @param templateId 渠道侧审核通过的模板 ID / 模板 Code
     * @throws SmsSendException 渠道返回失败或网络异常
     */
    void send(String mobile, String templateId, LinkedHashMap<String, String> params);

    /** 验证码短信即「验证码模板 + code 参数」 */
    @Override
    default void sendCode(String mobile, SmsScene scene, String code) {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put(CODE_PARAM, code);
        send(mobile, codeTemplate(), params);
    }

    /** 当前渠道配置的验证码模板 ID */
    String codeTemplate();
}
