package com.platform.cloud.mall.member.support;

import com.platform.cloud.mall.common.util.MaskUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 未接入短信渠道时的占位实现：开发模式把验证码打到日志，便于联调；非开发模式只告警不泄露验证码。
 */
@Slf4j
public class LogSmsSender implements SmsSender {

    private final boolean devMode;

    public LogSmsSender(boolean devMode) {
        this.devMode = devMode;
    }

    @Override
    public void sendCode(String mobile, SmsScene scene, String code) {
        if (devMode) {
            log.info("[模拟短信] mobile={} scene={} code={}", MaskUtils.mobile(mobile), scene, code);
        } else {
            log.warn("[模拟短信] 未接入短信渠道，验证码未实际发送 mobile={} scene={}", MaskUtils.mobile(mobile), scene);
        }
    }
}
