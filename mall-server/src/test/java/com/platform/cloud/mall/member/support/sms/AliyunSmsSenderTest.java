package com.platform.cloud.mall.member.support.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.tea.TeaException;
import com.platform.cloud.mall.member.support.SmsScene;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 阿里云短信：请求参数组装与成功 / 失败判定（mock SDK Client，不发真实请求）。
 */
class AliyunSmsSenderTest {

    private final SmsSettings settings = new SmsSettings(SmsProvider.ALIYUN, "ALIYUN", "LTAI5t", "secret",
            "优选商城", "", "", "SMS_CODE_1");

    @Test
    void sendCodeBuildsRequestWithSignTemplateAndJsonParam() throws Exception {
        Client client = mock(Client.class);
        when(client.sendSmsWithOptions(any(), any())).thenReturn(ok());
        new AliyunSmsSender(settings, client).sendCode("13800000000", SmsScene.LOGIN, "123456");

        ArgumentCaptor<SendSmsRequest> captor = ArgumentCaptor.forClass(SendSmsRequest.class);
        verify(client).sendSmsWithOptions(captor.capture(), any());
        SendSmsRequest req = captor.getValue();
        assertThat(req.getPhoneNumbers()).isEqualTo("13800000000");
        assertThat(req.getSignName()).isEqualTo("优选商城");
        assertThat(req.getTemplateCode()).isEqualTo("SMS_CODE_1");
        assertThat(req.getTemplateParam()).isEqualTo("{\"code\":\"123456\"}");
    }

    @Test
    void notifyParamsKeepInsertionOrder() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("orderNo", "O1");
        params.put("amount", "9.90");
        SendSmsRequest req = new AliyunSmsSender(settings, mock(Client.class))
                .buildRequest("13800000000", "SMS_PAY", params);
        assertThat(req.getTemplateParam()).isEqualTo("{\"orderNo\":\"O1\",\"amount\":\"9.90\"}");
    }

    @Test
    void nonOkCodeIsFailure() throws Exception {
        Client client = mock(Client.class);
        when(client.sendSmsWithOptions(any(), any())).thenReturn(new SendSmsResponse().setBody(
                new SendSmsResponseBody().setCode("isv.BUSINESS_LIMIT_CONTROL").setMessage("触发分钟级流控")));
        AliyunSmsSender sender = new AliyunSmsSender(settings, client);
        assertThatThrownBy(() -> sender.sendCode("13800000000", SmsScene.LOGIN, "1"))
                .isInstanceOf(SmsSendException.class)
                .hasMessageContaining("isv.BUSINESS_LIMIT_CONTROL");
    }

    @Test
    void sdkExceptionIsWrappedWithErrorCode() throws Exception {
        Client client = mock(Client.class);
        when(client.sendSmsWithOptions(any(), any()))
                .thenThrow(new TeaException(Map.of("code", "InvalidAccessKeyId.NotFound", "message", "x")));
        AliyunSmsSender sender = new AliyunSmsSender(settings, client);
        assertThatThrownBy(() -> sender.sendCode("13800000000", SmsScene.LOGIN, "1"))
                .isInstanceOf(SmsSendException.class)
                .hasMessageContaining("InvalidAccessKeyId.NotFound");
    }

    @Test
    void missingTemplateFailsBeforeCallingSdk() {
        SmsSettings noTpl = new SmsSettings(SmsProvider.ALIYUN, "ALIYUN", "a", "b", "s", "", "", "");
        assertThatThrownBy(() -> new AliyunSmsSender(noTpl, mock(Client.class))
                .sendCode("13800000000", SmsScene.LOGIN, "1"))
                .hasMessageContaining("模板");
    }

    private static SendSmsResponse ok() {
        return new SendSmsResponse().setBody(new SendSmsResponseBody().setCode("OK").setBizId("biz-1"));
    }
}
