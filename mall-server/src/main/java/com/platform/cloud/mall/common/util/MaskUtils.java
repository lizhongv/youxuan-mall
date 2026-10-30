package com.platform.cloud.mall.common.util;

/**
 * 敏感信息脱敏。后台列表、会员中心展示手机号时使用，防止截图、导出时泄露完整号码。
 */
public final class MaskUtils {

    private MaskUtils() {
    }

    /** 手机号保留前 3 后 4 位：13812345678 → 138****5678；长度不足 7 位时只保留首尾各 1 位 */
    public static String mobile(String mobile) {
        if (mobile == null || mobile.isEmpty()) {
            return mobile;
        }
        int len = mobile.length();
        if (len >= 7) {
            return mobile.substring(0, 3) + "****" + mobile.substring(len - 4);
        }
        if (len <= 2) {
            return "*".repeat(len);
        }
        return mobile.charAt(0) + "*".repeat(len - 2) + mobile.charAt(len - 1);
    }
}
