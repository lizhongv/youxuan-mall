package com.platform.cloud.mall.system.support.storage;

import java.util.Locale;

/**
 * 存储方式。S3 指任何兼容 S3 协议的对象存储：阿里云 OSS、腾讯云 COS、MinIO、七牛 Kodo、AWS S3。
 */
public enum StorageProvider {
    LOCAL,
    S3;

    /** 空值视为 LOCAL；无法识别返回 null，由调用方报错，避免误写到本地却以为上了云 */
    public static StorageProvider parse(String value) {
        if (value == null || value.isBlank()) {
            return LOCAL;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
