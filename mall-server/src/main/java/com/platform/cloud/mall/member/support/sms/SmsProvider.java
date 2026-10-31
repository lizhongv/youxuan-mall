package com.platform.cloud.mall.member.support.sms;

import java.util.Locale;

/**
 * 短信渠道。LOG 只把短信写日志，仅允许在开发 / 演示模式（mall.sms.dev-mode=true）下使用。
 */
public enum SmsProvider {
    LOG,
    ALIYUN,
    TENCENT;

    /** 后台手填的值容忍大小写与空白；无法识别返回 null，由调用方报错而不是悄悄回退到 LOG */
    public static SmsProvider parse(String value) {
        if (value == null || value.isBlank()) {
            return LOG;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
