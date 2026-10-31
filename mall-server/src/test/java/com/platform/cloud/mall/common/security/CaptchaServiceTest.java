package com.platform.cloud.mall.common.security;

import com.platform.cloud.mall.common.config.MallProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 图形验证码：答案存 Redis 2 分钟、按场景隔离、一次性、默认不回显答案；图片为可解码的 PNG。
 */
class CaptchaServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> ops;
    private MallProperties props;
    private CaptchaService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        props = new MallProperties();
        service = new CaptchaService(redis, props);
    }

    @Test
    void createStoresAnswerForTwoMinutesAndReturnsPng() {
        CaptchaVO vo = service.create(CaptchaService.SCOPE_ADMIN);

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(ops).set(eq("mall:captcha:admin:" + vo.getCaptchaId()), code.capture(), eq(Duration.ofMinutes(2)));
        assertThat(code.getValue()).matches("[2-9]{4}");
        assertThat(vo.getDevCode()).isNull();
        assertThat(vo.getImage()).startsWith("data:image/png;base64,");
        byte[] png = Base64.getDecoder().decode(vo.getImage().substring("data:image/png;base64,".length()));
        assertThat(png).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
    }

    @Test
    void echoOnlyWhenEnabled() {
        props.getSecurity().setCaptchaEcho(true);
        assertThat(service.create(CaptchaService.SCOPE_APP).getDevCode()).matches("[2-9]{4}");
    }

    @Test
    void verifyConsumesAnswerAtomically() {
        when(ops.getAndDelete("mall:captcha:app:abc")).thenReturn("2345", (String) null);

        assertThat(service.verify(CaptchaService.SCOPE_APP, "abc", " 2345 ")).isTrue();
        // 第二次使用同一个验证码：已被 GETDEL 删除
        assertThat(service.verify(CaptchaService.SCOPE_APP, "abc", "2345")).isFalse();
    }

    @Test
    void wrongAnswerAlsoInvalidatesCaptcha() {
        when(ops.getAndDelete("mall:captcha:app:abc")).thenReturn("2345");

        assertThat(service.verify(CaptchaService.SCOPE_APP, "abc", "9999")).isFalse();
        verify(ops).getAndDelete("mall:captcha:app:abc");
    }

    @Test
    void scopesAreIsolated() {
        when(ops.getAndDelete("mall:captcha:app:abc")).thenReturn("2345");

        assertThat(service.verify(CaptchaService.SCOPE_ADMIN, "abc", "2345")).isFalse();
    }

    @Test
    void blankInputIsRejectedWithoutTouchingRedis() {
        assertThat(service.verify(CaptchaService.SCOPE_APP, null, "2345")).isFalse();
        assertThat(service.verify(CaptchaService.SCOPE_APP, "abc", " ")).isFalse();
        verify(ops, never()).getAndDelete(anyString());
    }

    @Test
    void rendererProducesImagesForAllDigits() {
        assertThat(CaptchaImageRenderer.renderPng("0123456789")).isNotEmpty();
    }
}
