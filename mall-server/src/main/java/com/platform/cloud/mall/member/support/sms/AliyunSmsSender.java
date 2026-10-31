package com.platform.cloud.mall.member.support.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cloud.mall.common.util.MaskUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;

/**
 * 阿里云短信（dysmsapi 2017-05-25，官方 SDK com.aliyun:dysmsapi20170525）。
 * <p>
 * 文档：https://help.aliyun.com/zh/sms/developer-reference/api-dysmsapi-2017-05-25-sendsms
 * 成功判定是响应体 Code == "OK"；HTTP 200 不代表发送成功（如签名未审核、触发流控都返回 200）。
 */
@Slf4j
public class AliyunSmsSender implements SmsChannel {

    /** 短信服务是全局服务，所有地域共用这个 endpoint */
    static final String ENDPOINT = "dysmsapi.aliyuncs.com";
    static final String OK = "OK";
    /** 连接 3 秒、读取 5 秒：验证码接口同步等待渠道返回，超时太长会拖住用户请求线程 */
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 5000;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final SmsSettings settings;
    private final Client client;

    AliyunSmsSender(SmsSettings settings, Client client) {
        this.settings = settings;
        this.client = client;
    }

    /** 按配置构建 SDK 客户端。Client 线程安全，配置不变时复用同一个实例 */
    public static AliyunSmsSender create(SmsSettings settings) {
        Config config = new Config()
                .setAccessKeyId(settings.accessKeyId())
                .setAccessKeySecret(settings.accessKeySecret())
                .setEndpoint(ENDPOINT)
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setReadTimeout(READ_TIMEOUT_MS);
        if (!settings.region().isEmpty()) {
            config.setRegionId(settings.region());
        }
        try {
            return new AliyunSmsSender(settings, new Client(config));
        } catch (Exception e) {
            // SDK 构造只做参数检查，异常信息不含密钥
            throw new SmsSendException("阿里云短信客户端初始化失败：" + e.getMessage());
        }
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
            resp = client.sendSmsWithOptions(req, new RuntimeOptions());
        } catch (TeaException e) {
            log.error("阿里云短信调用失败 mobile={} template={} code={} msg={}",
                    MaskUtils.mobile(mobile), templateId, e.getCode(), e.getMessage());
            throw new SmsSendException("阿里云短信发送失败：" + e.getCode());
        } catch (Exception e) {
            log.error("阿里云短信调用异常 mobile={} template={}", MaskUtils.mobile(mobile), templateId, e);
            throw new SmsSendException("阿里云短信发送失败：网络异常");
        }
        SendSmsResponseBody body = resp == null ? null : resp.getBody();
        if (body == null || !OK.equalsIgnoreCase(body.getCode())) {
            String code = body == null ? "EMPTY_RESPONSE" : body.getCode();
            String msg = body == null ? "" : body.getMessage();
            log.error("阿里云短信发送失败 mobile={} template={} code={} msg={} requestId={}",
                    MaskUtils.mobile(mobile), templateId, code, msg, body == null ? null : body.getRequestId());
            throw new SmsSendException("阿里云短信发送失败：" + code + (msg == null || msg.isEmpty() ? "" : "（" + msg + "）"));
        }
        log.info("阿里云短信已发送 mobile={} template={} bizId={}", MaskUtils.mobile(mobile), templateId, body.getBizId());
    }

    SendSmsRequest buildRequest(String mobile, String templateId, LinkedHashMap<String, String> params) {
        SendSmsRequest req = new SendSmsRequest()
                .setPhoneNumbers(mobile)
                .setSignName(settings.signName())
                .setTemplateCode(templateId);
        if (params != null && !params.isEmpty()) {
            try {
                req.setTemplateParam(JSON.writeValueAsString(params));
            } catch (JsonProcessingException e) {
                throw new SmsSendException("短信参数序列化失败");
            }
        }
        return req;
    }
}
