package com.platform.cloud.mall.member.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.member.config.SmsProperties;
import com.platform.cloud.mall.member.entity.MbrMember;
import com.platform.cloud.mall.member.mapper.MbrMemberMapper;
import com.platform.cloud.mall.member.support.SmsScene;
import com.platform.cloud.mall.member.support.SmsSender;
import com.platform.cloud.mall.member.vo.SmsCodeVO;
import com.platform.cloud.mall.testsupport.TableInfoTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证码规则：6 位、5 分钟、60 秒间隔、错误 5 次作废、场景隔离、开发模式回显。
 */
class SmsCodeServiceImplTest {

    private static final String MOBILE = "13800001234";
    private static final String CODE_KEY = "mall:sms:code:LOGIN:" + MOBILE;
    private static final String FAIL_KEY = "mall:sms:fail:LOGIN:" + MOBILE;

    private StringRedisTemplate redis;
    private ValueOperations<String, String> ops;
    private MbrMemberMapper memberMapper;
    private SmsSender sender;
    private SmsProperties props;
    private SmsCodeServiceImpl service;

    @BeforeAll
    static void initTables() {
        TableInfoTestSupport.init(MbrMember.class);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        memberMapper = mock(MbrMemberMapper.class);
        sender = mock(SmsSender.class);
        props = new SmsProperties();
        service = new SmsCodeServiceImpl(redis, memberMapper, sender, props);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(ops.increment(anyString())).thenReturn(1L);
        when(memberMapper.selectCount(any())).thenReturn(0L);
    }

    @Test
    void sendStoresSixDigitCodeForFiveMinutesAndReturnsDevCode() {
        SmsCodeVO vo = service.send(MOBILE, SmsScene.LOGIN);

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(ops).set(eq(CODE_KEY), code.capture(), eq(Duration.ofMinutes(5)));
        assertThat(code.getValue()).matches("\\d{6}");
        assertThat(vo.getDevCode()).isEqualTo(code.getValue());
        verify(ops).setIfAbsent("mall:sms:interval:" + MOBILE, "1", Duration.ofSeconds(60));
        verify(sender).sendCode(MOBILE, SmsScene.LOGIN, code.getValue());
    }

    @Test
    void devCodeHiddenWhenDevModeOff() {
        props.setDevMode(false);
        assertThat(service.send(MOBILE, SmsScene.LOGIN).getDevCode()).isNull();
    }

    @Test
    void secondSendWithinSixtySecondsIsRejected() {
        when(ops.setIfAbsent(eq("mall:sms:interval:" + MOBILE), anyString(), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> service.send(MOBILE, SmsScene.LOGIN))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("60 秒");
        verify(sender, never()).sendCode(anyString(), any(), anyString());
    }

    @Test
    void dailyLimitIsEnforced() {
        when(ops.increment(anyString())).thenReturn(11L);
        assertThatThrownBy(() -> service.send(MOBILE, SmsScene.LOGIN)).hasMessageContaining("上限");
    }

    @Test
    void registerSceneRejectsRegisteredMobile() {
        when(memberMapper.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> service.send(MOBILE, SmsScene.REGISTER)).hasMessageContaining("已注册");
    }

    @Test
    void resetSceneRequiresRegisteredMobile() {
        assertThatThrownBy(() -> service.send(MOBILE, SmsScene.RESET)).hasMessageContaining("未注册");
    }

    @Test
    void senderFailureReleasesIntervalLock() {
        doThrow(new RuntimeException("channel down")).when(sender).sendCode(anyString(), any(), anyString());

        assertThatThrownBy(() -> service.send(MOBILE, SmsScene.LOGIN)).hasMessageContaining("短信发送失败");
        verify(redis).delete("mall:sms:interval:" + MOBILE);
        verify(redis).delete(CODE_KEY);
    }

    @Test
    void correctCodeIsConsumedOnce() {
        when(ops.get(CODE_KEY)).thenReturn("123456");

        service.verify(MOBILE, SmsScene.LOGIN, "123456");

        verify(redis).delete(CODE_KEY);
        verify(redis).delete(FAIL_KEY);
    }

    @Test
    void expiredCodeIsRejected() {
        when(ops.get(CODE_KEY)).thenReturn(null);
        assertThatThrownBy(() -> service.verify(MOBILE, SmsScene.LOGIN, "123456")).hasMessageContaining("已失效");
    }

    @Test
    void wrongCodeCountsFailures() {
        when(ops.get(CODE_KEY)).thenReturn("123456");
        when(ops.increment(FAIL_KEY)).thenReturn(1L);

        assertThatThrownBy(() -> service.verify(MOBILE, SmsScene.LOGIN, "000000")).hasMessage("验证码错误");
        verify(redis, never()).delete(CODE_KEY);
    }

    @Test
    void fifthWrongAttemptInvalidatesCode() {
        when(ops.get(CODE_KEY)).thenReturn("123456");
        when(ops.increment(FAIL_KEY)).thenReturn(5L);

        assertThatThrownBy(() -> service.verify(MOBILE, SmsScene.LOGIN, "000000")).hasMessageContaining("次数过多");
        verify(redis).delete(CODE_KEY);
    }

    @Test
    void codeOfAnotherSceneDoesNotWork() {
        when(ops.get(CODE_KEY)).thenReturn("123456");
        when(ops.get("mall:sms:code:RESET:" + MOBILE)).thenReturn(null);

        assertThatThrownBy(() -> service.verify(MOBILE, SmsScene.RESET, "123456")).hasMessageContaining("已失效");
    }

    @Test
    void generatedCodesAreAlwaysSixDigits() {
        for (int i = 0; i < 1000; i++) {
            assertThat(SmsCodeServiceImpl.generateCode()).matches("\\d{6}");
        }
    }
}
