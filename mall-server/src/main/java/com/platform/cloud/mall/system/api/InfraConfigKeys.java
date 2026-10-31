package com.platform.cloud.mall.system.api;

/**
 * 第三方基础设施（短信 / 对象存储 / 物流查询）在 sys_config 中的键，默认值见 Flyway V7__infra.sql。
 * <p>
 * 与 {@link ConfigKeys} 分开放：这些键只被基础设施适配层读取，单独成类可以和运营参数的改动互不干扰。
 * 运行时每次调用都从 {@link ConfigApi} 读取，后台修改后无需重启即可生效。
 */
public final class InfraConfigKeys {

    // ---------------- 短信 ----------------
    /** 短信渠道：LOG（仅开发/演示）/ ALIYUN / TENCENT */
    public static final String SMS_PROVIDER = "sms.provider";
    /** 阿里云 AccessKeyId / 腾讯云 SecretId */
    public static final String SMS_ACCESS_KEY_ID = "sms.access_key_id";
    /** 阿里云 AccessKeySecret / 腾讯云 SecretKey（敏感） */
    public static final String SMS_ACCESS_KEY_SECRET = "sms.access_key_secret";
    /** 短信签名（审核通过的签名名称，不含【】） */
    public static final String SMS_SIGN_NAME = "sms.sign_name";
    /** 地域：腾讯云如 ap-guangzhou；阿里云如 cn-hangzhou，留空用默认 */
    public static final String SMS_REGION = "sms.region";
    /** 腾讯云短信 SdkAppId */
    public static final String SMS_TENCENT_SDK_APP_ID = "sms.tencent.sdk_app_id";
    /** 验证码模板 ID */
    public static final String SMS_TEMPLATE_CODE = "sms.template.code";
    /** 通知模板：支付成功 */
    public static final String SMS_TEMPLATE_PAY_SUCCESS = "sms.template.pay_success";
    /** 通知模板：订单发货 */
    public static final String SMS_TEMPLATE_DELIVERED = "sms.template.delivered";
    /** 通知模板：退款成功 */
    public static final String SMS_TEMPLATE_REFUND_SUCCESS = "sms.template.refund_success";

    // ---------------- 对象存储 ----------------
    /** 存储方式：LOCAL / S3 */
    public static final String STORAGE_PROVIDER = "storage.provider";
    /** S3 协议 endpoint，如 https://oss-cn-hangzhou.aliyuncs.com */
    public static final String STORAGE_S3_ENDPOINT = "storage.s3.endpoint";
    /** 地域，如 oss-cn-hangzhou / ap-guangzhou / us-east-1 */
    public static final String STORAGE_S3_REGION = "storage.s3.region";
    /** 桶名 */
    public static final String STORAGE_S3_BUCKET = "storage.s3.bucket";
    /** AccessKey */
    public static final String STORAGE_S3_ACCESS_KEY = "storage.s3.access_key";
    /** SecretKey（敏感） */
    public static final String STORAGE_S3_SECRET_KEY = "storage.s3.secret_key";
    /** 是否 path-style 访问：MinIO 一般为 true；OSS 只支持 virtual-hosted，必须为 false */
    public static final String STORAGE_S3_PATH_STYLE = "storage.s3.path_style";
    /** 对外访问域名（CDN 或桶域名），留空则按 endpoint + bucket 推导 */
    public static final String STORAGE_PUBLIC_BASE_URL = "storage.public_base_url";
    /** 对象键前缀，如 mall，便于同一个桶放多个项目 */
    public static final String STORAGE_PATH_PREFIX = "storage.path_prefix";

    // ---------------- 物流 ----------------
    /** 物流查询渠道：NONE / KUAIDI100 */
    public static final String LOGISTICS_PROVIDER = "logistics.provider";
    /** 快递100 customer */
    public static final String LOGISTICS_KUAIDI100_CUSTOMER = "logistics.kuaidi100.customer";
    /** 快递100 授权 key（敏感） */
    public static final String LOGISTICS_KUAIDI100_KEY = "logistics.kuaidi100.key";

    private InfraConfigKeys() {
    }
}
