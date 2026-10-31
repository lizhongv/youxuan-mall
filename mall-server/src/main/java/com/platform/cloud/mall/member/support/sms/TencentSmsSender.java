package com.platform.cloud.mall.member.support.sms;

import com.platform.cloud.mall.common.util.MaskUtils;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;

/**
 * 腾讯云短信（API 2021-01-11，官方 SDK com.tencentcloudapi:tencentcloud-sdk-java-sms）。
 * <p>
 * 文档：https://cloud.tencent.com/document/api/382/55981
 * 成功判定是 SendStatusSet[0].Code == "Ok"；接口整体成功不代表这一条发送成功（如号码格式错误单独报在 SendStatus 里）。
 * 手机号必须是 E.164 格式（+86 前缀），模板参数按模板中 {1}{2}… 的位置传入。
 */
@Slf4j
public class TencentSmsSender implements SmsChannel {

    static final String ENDPOINT = "sms.tencentcloudapi.com";
    /** 未填地域时用广州：腾讯云短信国内站只在 ap-guangzhou / ap-beijing / ap-nanjing 提供，广州为官方示例默认 */
    static final String DEFAULT_REGION = "ap-guangzhou";
    static final String OK = "Ok";
    /** 腾讯云 SDK 超时单位是秒；理由同阿里云：验证码同步发送，不能长时间阻塞 */
    private static final int CONNECT_TIMEOUT_SECONDS = 3;
    private static final int READ_TIMEOUT_SECONDS = 5;
    private static final String CN_PREFIX = "+86";

    private final SmsSettings settings;
    private final SmsClient client;

    TencentSmsSender(SmsSettings settings, SmsClient client) {
        this.settings = settings;
        this.client = client;
    }

    /** 按配置构建 SDK 客户端，SmsClient 线程安全可复用 */
    public static TencentSmsSender create(SmsSettings settings) {
        Credential cred = new Credential(settings.accessKeyId(), settings.accessKeySecret());
        HttpProfile http = new HttpProfile();
        http.setEndpoint(ENDPOINT);
        http.setConnTimeout(CONNECT_TIMEOUT_SECONDS);
        http.setReadTimeout(READ_TIMEOUT_SECONDS);
        ClientProfile profile = new ClientProfile();
        profile.setHttpProfile(http);
        String region = settings.region().isEmpty() ? DEFAULT_REGION : settings.region();
        return new TencentSmsSender(settings, new SmsClient(cred, region, profile));
    }

    @Override
    public String codeTemplate() {
        return settings.codeTemplate();
    }

    @Override
    public void send(String mobile, String templateId, LinkedHashMap<String, String> params) {
        if (templateId == null || templateId.isBlank()) {
            throw new SmsSendException("未配置短信模板");
        }
        SendSmsRequest req = buildRequest(mobile, templateId, params);
        SendSmsResponse resp;
        try {
            resp = client.SendSms(req);
        } catch (TencentCloudSDKException e) {
            log.error("腾讯云短信调用失败 mobile={} template={} code={} msg={} requestId={}",
                    MaskUtils.mobile(mobile), templateId, e.getErrorCode(), e.getMessage(), e.getRequestId());
            throw new SmsSendException("腾讯云短信发送失败：" + (e.getErrorCode() == null ? "网络异常" : e.getErrorCode()));
        }
        SendStatus[] statuses = resp == null ? null : resp.getSendStatusSet();
        SendStatus status = statuses == null || statuses.length == 0 ? null : statuses[0];
        if (status == null || !OK.equalsIgnoreCase(status.getCode())) {
            String code = status == null ? "EMPTY_RESPONSE" : status.getCode();
            String msg = status == null ? "" : status.getMessage();
            log.error("腾讯云短信发送失败 mobile={} template={} code={} msg={} requestId={}",
                    MaskUtils.mobile(mobile), templateId, code, msg, resp == null ? null : resp.getRequestId());
            throw new SmsSendException("腾讯云短信发送失败：" + code + (msg == null || msg.isEmpty() ? "" : "（" + msg + "）"));
        }
        log.info("腾讯云短信已发送 mobile={} template={} serialNo={}",
                MaskUtils.mobile(mobile), templateId, status.getSerialNo());
    }

    SendSmsRequest buildRequest(String mobile, String templateId, LinkedHashMap<String, String> params) {
        SendSmsRequest req = new SendSmsRequest();
        req.setSmsSdkAppId(settings.sdkAppId());
        req.setSignName(settings.signName());
        req.setTemplateId(templateId);
        req.setPhoneNumberSet(new String[]{toE164(mobile)});
        if (params != null && !params.isEmpty()) {
            req.setTemplateParamSet(params.values().toArray(new String[0]));
        }
        return req;
    }

    /** 本系统只接受国内 11 位手机号（见 SmsCodeRequest），统一补 +86；已带 + 的原样传 */
    static String toE164(String mobile) {
        return mobile.startsWith("+") ? mobile : CN_PREFIX + mobile;
    }
}
