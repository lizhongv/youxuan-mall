package com.platform.cloud.mall.system.support.storage;

import com.platform.cloud.mall.system.api.ConfigApi;
import com.platform.cloud.mall.system.api.InfraConfigKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 对象存储配置快照。equals 用来判断是否需要重建 S3 客户端；toString 不含 SecretKey。
 *
 * @param provider 解析后的存储方式，无法识别为 null
 */
public record S3Settings(StorageProvider provider,
                         String rawProvider,
                         String endpoint,
                         String region,
                         String bucket,
                         String accessKey,
                         String secretKey,
                         boolean pathStyle,
                         String publicBaseUrl,
                         String pathPrefix) {

    /** 未填地域时的占位：MinIO 等自建服务不校验地域，但 SDK 要求必须有值 */
    static final String DEFAULT_REGION = "us-east-1";

    public static S3Settings from(ConfigApi config) {
        String raw = config.getString(InfraConfigKeys.STORAGE_PROVIDER, "LOCAL");
        String region = trim(config.getString(InfraConfigKeys.STORAGE_S3_REGION, ""));
        return new S3Settings(
                StorageProvider.parse(raw),
                raw,
                stripTrailingSlash(trim(config.getString(InfraConfigKeys.STORAGE_S3_ENDPOINT, ""))),
                region.isEmpty() ? DEFAULT_REGION : region,
                trim(config.getString(InfraConfigKeys.STORAGE_S3_BUCKET, "")),
                trim(config.getString(InfraConfigKeys.STORAGE_S3_ACCESS_KEY, "")),
                trim(config.getString(InfraConfigKeys.STORAGE_S3_SECRET_KEY, "")),
                Boolean.parseBoolean(trim(config.getString(InfraConfigKeys.STORAGE_S3_PATH_STYLE, "false"))
                        .toLowerCase(Locale.ROOT)),
                stripTrailingSlash(trim(config.getString(InfraConfigKeys.STORAGE_PUBLIC_BASE_URL, ""))),
                normalizePrefix(config.getString(InfraConfigKeys.STORAGE_PATH_PREFIX, "")));
    }

    /** 缺失的必填项名称（不含值） */
    public List<String> missingFields() {
        List<String> missing = new ArrayList<>();
        if (endpoint.isEmpty()) {
            missing.add(InfraConfigKeys.STORAGE_S3_ENDPOINT);
        }
        if (bucket.isEmpty()) {
            missing.add(InfraConfigKeys.STORAGE_S3_BUCKET);
        }
        if (accessKey.isEmpty()) {
            missing.add(InfraConfigKeys.STORAGE_S3_ACCESS_KEY);
        }
        if (secretKey.isEmpty()) {
            missing.add(InfraConfigKeys.STORAGE_S3_SECRET_KEY);
        }
        return missing;
    }

    @Override
    public String toString() {
        return "S3Settings[provider=" + provider + ", endpoint=" + endpoint + ", region=" + region
                + ", bucket=" + bucket + ", pathStyle=" + pathStyle + ", publicBaseUrl=" + publicBaseUrl
                + ", pathPrefix=" + pathPrefix + "]";
    }

    /** 前缀去掉首尾斜杠与反斜杠：「/mall/」「mall」都视为 mall，空表示不加前缀 */
    static String normalizePrefix(String prefix) {
        String p = trim(prefix).replace('\\', '/');
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    private static String stripTrailingSlash(String s) {
        String r = s;
        while (r.endsWith("/")) {
            r = r.substring(0, r.length() - 1);
        }
        return r;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
