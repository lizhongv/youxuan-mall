package com.platform.cloud.mall.member.support.sms;

import com.platform.cloud.mall.member.support.SmsScene;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 腾讯云短信：E.164 手机号、按位置传参、SendStatus 判定（mock SmsClient）。
 */
class TencentSmsSenderTest {

    private final SmsSettings settings = new SmsSettings(SmsProvider.TENCENT, "TENCENT", "AKID", "secret",
            "优选商城", "ap-guangzhou", "1400123456", "1234567");

    @Test
    void sendCodeBuildsRequestWithAppIdAndE164Mobile() throws Exception {
        SmsClient client = mock(SmsClient.class);
        when(client.SendSms(any())).thenReturn(response("Ok"));
        new TencentSmsSender(settings, client).sendCode("13800000000", SmsScene.LOGIN, "654321");

        ArgumentCaptor<SendSmsRequest> captor = ArgumentCaptor.forClass(SendSmsRequest.class);
        verify(client).SendSms(captor.capture());
        SendSmsRequest req = captor.getValue();
        assertThat(req.getSmsSdkAppId()).isEqualTo("1400123456");
        assertThat(req.getSignName()).isEqualTo("优选商城");
        assertThat(req.getTemplateId()).isEqualTo("1234567");
        assertThat(req.getPhoneNumberSet()).containsExactly("+8613800000000");
        assertThat(req.getTemplateParamSet()).containsExactly("654321");
    }

    @Test
    void notifyParamsArePositionalInInsertionOrder() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("orderNo", "O1");
        params.put("company", "顺丰速运");
        params.put("trackingNo", "SF100");
        SendSmsRequest req = new TencentSmsSender(settings, mock(SmsClient.class))
                .buildRequest("13800000000", "777", params);
        assertThat(req.getTemplateParamSet()).containsExactly("O1", "顺丰速运", "SF100");
    }

    @Test
    void failedSendStatusIsFailure() throws Exception {
        SmsClient client = mock(SmsClient.class);
        when(client.SendSms(any())).thenReturn(response("LimitExceeded.PhoneNumberDailyLimit"));
        TencentSmsSender sender = new TencentSmsSender(settings, client);
        assertThatThrownBy(() -> sender.sendCode("13800000000", SmsScene.LOGIN, "1"))
                .isInstanceOf(SmsSendException.class)
                .hasMessageContaining("LimitExceeded.PhoneNumberDailyLimit");
    }

    @Test
    void sdkExceptionIsWrappedWithErrorCode() throws Exception {
        SmsClient client = mock(SmsClient.class);
        when(client.SendSms(any())).thenThrow(
                new TencentCloudSDKException("签名错误", "req-1", "AuthFailure.SignatureFailure"));
        TencentSmsSender sender = new TencentSmsSender(settings, client);
        assertThatThrownBy(() -> sender.sendCode("13800000000", SmsScene.LOGIN, "1"))
                .isInstanceOf(SmsSendException.class)
                .hasMessageContaining("AuthFailure.SignatureFailure");
    }

    @Test
    void e164KeepsExistingPlusPrefix() {
        assertThat(TencentSmsSender.toE164("13800000000")).isEqualTo("+8613800000000");
        assertThat(TencentSmsSender.toE164("+85212345678")).isEqualTo("+85212345678");
    }

    private static SendSmsResponse response(String code) {
        SendStatus status = new SendStatus();
        status.setCode(code);
        status.setMessage("msg");
        SendSmsResponse resp = new SendSmsResponse();
        resp.setSendStatusSet(new SendStatus[]{status});
        return resp;
    }
}
