package com.platform.cloud.mall.member.service.impl;

import com.platform.cloud.mall.common.util.MaskUtils;
import com.platform.cloud.mall.member.service.NotifySmsService;
import com.platform.cloud.mall.member.support.SmsScene;
import com.platform.cloud.mall.member.support.sms.RoutingSmsSender;
import com.platform.cloud.mall.member.support.sms.SmsProvider;
import com.platform.cloud.mall.member.support.sms.SmsTemplate;
import com.platform.cloud.mall.system.api.ConfigApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.LinkedHashMap;

/**
 * 通知短信：查模板 ID → 交给 RoutingSmsSender 发送，失败吞掉只记日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifySmsServiceImpl implements NotifySmsService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RoutingSmsSender sender;
    private final ConfigApi config;

    @Override
    public boolean send(String mobile, SmsTemplate template, LinkedHashMap<String, String> params) {
        if (mobile == null || mobile.isBlank()) {
            return false;
        }
        String templateId = config.getString(template.configKey(), "").trim();
        if (templateId.isEmpty()) {
            // 未配置模板即视为关闭该类通知，属于正常情况，不报警
            log.debug("通知短信 {} 未配置模板，跳过", template);
            return false;
        }
        try {
            sender.send(mobile, templateId, params == null ? new LinkedHashMap<>() : params);
            return true;
        } catch (RuntimeException e) {
            log.warn("通知短信发送失败 template={} mobile={} reason={}", template, MaskUtils.mobile(mobile), e.getMessage());
            return false;
        }
    }

    @Override
    public SmsProvider sendTest(String mobile) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        // 测试码不写入 Redis，不能用来登录，只验证渠道连通、签名与模板是否审核通过
        sender.sendCode(mobile, SmsScene.LOGIN, code);
        return sender.currentProvider();
    }
}
