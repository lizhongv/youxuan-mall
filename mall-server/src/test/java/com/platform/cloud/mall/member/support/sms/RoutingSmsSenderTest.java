package com.platform.cloud.mall.member.support.sms;

import com.platform.cloud.mall.member.config.SmsProperties;
import com.platform.cloud.mall.member.support.SmsScene;
import com.platform.cloud.mall.system.api.InfraConfigKeys;
import com.platform.cloud.mall.testsupport.FakeConfigApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 短信路由：按配置分发、配置不变复用客户端、配置变化重建、LOG 仅限开发模式。
 */
class RoutingSmsSenderTest {

    private FakeConfigApi config;
    private SmsProperties props;
    private List<SmsSettings> built;
    private List<SmsChannel> channels;
    private Function<SmsSettings, SmsChannel> factory;

    @BeforeEach
    void setUp() {
        config = new FakeConfigApi();
        props = new SmsProperties();
        built = new ArrayList<>();
        channels = new ArrayList<>();
        factory = s -> {
            built.add(s);
            SmsChannel c = mock(SmsChannel.class);
            channels.add(c);
            return c;
        };
    }

    @Test
    void logProviderIsAllowedOnlyInDevMode() {
        config.put(InfraConfigKeys.SMS_PROVIDER, "LOG");
        props.setDevMode(true);
        new RoutingSmsSender(config, props, factory).sendCode("13800000000", SmsScene.LOGIN, "123456");
        assertThat(built).isEmpty();

        props.setDevMode(false);
        RoutingSmsSender prod = new RoutingSmsSender(config, props, factory);
        assertThatThrownBy(() -> prod.sendCode("13800000000", SmsScene.LOGIN, "123456"))
                .isInstanceOf(SmsSendException.class)
                .hasMessageContaining("未开通");
        assertThatThrownBy(() -> prod.send("13800000000", "T1", new LinkedHashMap<>()))
                .isInstanceOf(SmsSendException.class);
    }

    @Test
    void routesToConfiguredProviderAndReusesClientUntilConfigChanges() {
        aliyun();
        RoutingSmsSender sender = new RoutingSmsSender(config, props, factory);

        sender.sendCode("13800000000", SmsScene.LOGIN, "111111");
        sender.sendCode("13800000001", SmsScene.REGISTER, "222222");
        assertThat(built).hasSize(1);
        assertThat(built.get(0).provider()).isEqualTo(SmsProvider.ALIYUN);
        verify(channels.get(0)).sendCode("13800000001", SmsScene.REGISTER, "222222");

        // 后台改签名：重建客户端，新签名立即生效
        config.put(InfraConfigKeys.SMS_SIGN_NAME, "新签名");
        sender.sendCode("13800000000", SmsScene.LOGIN, "333333");
        assertThat(built).hasSize(2);
        assertThat(built.get(1).signName()).isEqualTo("新签名");

        // 切换到腾讯云
        config.put(InfraConfigKeys.SMS_PROVIDER, "tencent").put(InfraConfigKeys.SMS_TENCENT_SDK_APP_ID, "1400000000");
        sender.send("13800000000", "T9", new LinkedHashMap<>());
        assertThat(built).hasSize(3);
        assertThat(built.get(2).provider()).isEqualTo(SmsProvider.TENCENT);
        verify(channels.get(2)).send(eq("13800000000"), eq("T9"), any());
    }

    @Test
    void incompleteConfigFailsWithMissingKeyNamesOnly() {
        config.put(InfraConfigKeys.SMS_PROVIDER, "TENCENT")
                .put(InfraConfigKeys.SMS_ACCESS_KEY_ID, "AKID")
                .put(InfraConfigKeys.SMS_ACCESS_KEY_SECRET, "top-secret");
        RoutingSmsSender sender = new RoutingSmsSender(config, props, factory);
        assertThatThrownBy(() -> sender.sendCode("13800000000", SmsScene.LOGIN, "1"))
                .isInstanceOf(SmsSendException.class)
                .hasMessageContaining(InfraConfigKeys.SMS_SIGN_NAME)
                .hasMessageContaining(InfraConfigKeys.SMS_TENCENT_SDK_APP_ID)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("top-secret"));
        assertThat(built).isEmpty();
    }

    @Test
    void unknownProviderIsRejectedInsteadOfFallingBackToLog() {
        config.put(InfraConfigKeys.SMS_PROVIDER, "HUAWEI");
        RoutingSmsSender sender = new RoutingSmsSender(config, props, factory);
        assertThatThrownBy(() -> sender.sendCode("13800000000", SmsScene.LOGIN, "1"))
                .hasMessageContaining("HUAWEI");
    }

    @Test
    void settingsToStringNeverContainsSecret() {
        aliyun();
        String text = SmsSettings.from(config).toString();
        assertThat(text).doesNotContain("secret-value").contains("LTAI****");
    }

    private void aliyun() {
        config.put(InfraConfigKeys.SMS_PROVIDER, "ALIYUN")
                .put(InfraConfigKeys.SMS_ACCESS_KEY_ID, "LTAI5tExample")
                .put(InfraConfigKeys.SMS_ACCESS_KEY_SECRET, "secret-value")
                .put(InfraConfigKeys.SMS_SIGN_NAME, "优选商城")
                .put(InfraConfigKeys.SMS_TEMPLATE_CODE, "SMS_123");
    }
}
