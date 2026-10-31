package com.platform.cloud.mall.member.support.sms;

import com.platform.cloud.mall.system.api.ConfigApi;
import com.platform.cloud.mall.system.api.InfraConfigKeys;

import java.util.ArrayList;
import java.util.List;

/**
 * 某一时刻的短信配置快照。record 的 equals 用于判断「配置是否变化、是否需要重建 SDK 客户端」。
 * <p>
 * toString 已重写为不含密钥，防止被日志或异常信息带出。
 *
 * @param rawProvider 后台填写的原值，用于无法识别时提示
 */
public record SmsSettings(SmsProvider provider,
                          String rawProvider,
                          String accessKeyId,
                          String accessKeySecret,
                          String signName,
                          String region,
                          String sdkAppId,
                          String codeTemplate) {

    /** 从 sys_config 读取当前配置；ConfigApi 自带本地缓存，每次发送都调用也没有数据库压力 */
    public static SmsSettings from(ConfigApi config) {
        String raw = config.getString(InfraConfigKeys.SMS_PROVIDER, "LOG");
        return new SmsSettings(
                SmsProvider.parse(raw),
                raw,
                trim(config.getString(InfraConfigKeys.SMS_ACCESS_KEY_ID, "")),
                trim(config.getString(InfraConfigKeys.SMS_ACCESS_KEY_SECRET, "")),
                trim(config.getString(InfraConfigKeys.SMS_SIGN_NAME, "")),
                trim(config.getString(InfraConfigKeys.SMS_REGION, "")),
                trim(config.getString(InfraConfigKeys.SMS_TENCENT_SDK_APP_ID, "")),
                trim(config.getString(InfraConfigKeys.SMS_TEMPLATE_CODE, "")));
    }

    /** 列出缺失的必填项（只给名称，不含值），配置不全时给出可操作的提示 */
    public List<String> missingFields() {
        List<String> missing = new ArrayList<>();
        if (provider == SmsProvider.ALIYUN || provider == SmsProvider.TENCENT) {
            if (accessKeyId.isEmpty()) {
                missing.add(InfraConfigKeys.SMS_ACCESS_KEY_ID);
            }
            if (accessKeySecret.isEmpty()) {
                missing.add(InfraConfigKeys.SMS_ACCESS_KEY_SECRET);
            }
            if (signName.isEmpty()) {
                missing.add(InfraConfigKeys.SMS_SIGN_NAME);
            }
        }
        if (provider == SmsProvider.TENCENT && sdkAppId.isEmpty()) {
            missing.add(InfraConfigKeys.SMS_TENCENT_SDK_APP_ID);
        }
        return missing;
    }

    @Override
    public String toString() {
        return "SmsSettings[provider=" + provider + ", accessKeyId=" + maskId(accessKeyId)
                + ", signName=" + signName + ", region=" + region + ", sdkAppId=" + sdkAppId
                + ", codeTemplate=" + codeTemplate + "]";
    }

    /** AccessKeyId 本身不算机密，但也只露前 4 位，足够排查「填错了哪个账号」 */
    static String maskId(String id) {
        if (id == null || id.length() <= 4) {
            return "****";
        }
        return id.substring(0, 4) + "****";
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
