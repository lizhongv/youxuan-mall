package com.platform.cloud.mall.member.service.impl;

import com.platform.cloud.mall.member.support.SmsScene;
import com.platform.cloud.mall.member.support.sms.RoutingSmsSender;
import com.platform.cloud.mall.member.support.sms.SmsProvider;
import com.platform.cloud.mall.member.support.sms.SmsSendException;
import com.platform.cloud.mall.member.support.sms.SmsTemplate;
import com.platform.cloud.mall.system.api.InfraConfigKeys;
import com.platform.cloud.mall.testsupport.FakeConfigApi;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知短信：未配置模板即跳过；渠道失败不向业务方抛异常。
 */
class NotifySmsServiceImplTest {

    private final RoutingSmsSender sender = mock(RoutingSmsSender.class);
    private final FakeConfigApi config = new FakeConfigApi();
    private final NotifySmsServiceImpl service = new NotifySmsServiceImpl(sender, config);

    @Test
    void blankTemplateMeansDisabled() {
        assertThat(service.send("13800000000", SmsTemplate.DELIVERED, new LinkedHashMap<>())).isFalse();
        verify(sender, never()).send(anyString(), anyString(), any());
    }

    @Test
    void sendsWithConfiguredTemplateId() {
        config.put(InfraConfigKeys.SMS_TEMPLATE_PAY_SUCCESS, " SMS_PAY ");
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("orderNo", "O1");
        assertThat(service.send("13800000000", SmsTemplate.PAY_SUCCESS, params)).isTrue();
        verify(sender).send("13800000000", "SMS_PAY", params);
    }

    @Test
    void channelFailureIsSwallowed() {
        config.put(InfraConfigKeys.SMS_TEMPLATE_REFUND_SUCCESS, "SMS_REFUND");
        doThrow(new SmsSendException("x")).when(sender).send(anyString(), anyString(), any());
        assertThat(service.send("13800000000", SmsTemplate.REFUND_SUCCESS, null)).isFalse();
    }

    @Test
    void testSendUsesCodeTemplateWithRandomSixDigits() {
        when(sender.currentProvider()).thenReturn(SmsProvider.ALIYUN);
        assertThat(service.sendTest("13800000000")).isEqualTo(SmsProvider.ALIYUN);
        verify(sender).sendCode(eq("13800000000"), eq(SmsScene.LOGIN),
                org.mockito.ArgumentMatchers.matches("^\\d{6}$"));
    }
}
