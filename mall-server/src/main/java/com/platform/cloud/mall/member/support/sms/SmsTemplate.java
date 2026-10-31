package com.platform.cloud.mall.member.support.sms;

import com.platform.cloud.mall.system.api.InfraConfigKeys;

/**
 * 通知短信模板。模板 ID 在后台系统参数中配置，留空即表示不发送该类通知。
 * <p>
 * 建议参数（按此顺序放入 LinkedHashMap，腾讯云模板变量按同样顺序写 {1}{2}{3}）：
 * <ul>
 *   <li>PAY_SUCCESS：orderNo, amount</li>
 *   <li>DELIVERED：orderNo, company, trackingNo</li>
 *   <li>REFUND_SUCCESS：orderNo, amount</li>
 * </ul>
 */
public enum SmsTemplate {
    PAY_SUCCESS(InfraConfigKeys.SMS_TEMPLATE_PAY_SUCCESS),
    DELIVERED(InfraConfigKeys.SMS_TEMPLATE_DELIVERED),
    REFUND_SUCCESS(InfraConfigKeys.SMS_TEMPLATE_REFUND_SUCCESS);

    private final String configKey;

    SmsTemplate(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }
}
