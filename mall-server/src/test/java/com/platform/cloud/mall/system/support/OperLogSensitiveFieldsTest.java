package com.platform.cloud.mall.system.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 操作日志不记录令牌、验证码、密钥。
 */
class OperLogSensitiveFieldsTest {

    @Test
    void tokensCaptchasAndSecretsAreMasked() {
        String json = OperLogParams.serialize(new ObjectMapper(), new Object[]{Map.of(
                "refreshToken", "eyJhbGciOi...",
                "captchaCode", "2345",
                "apiSecret", "s3cr3t",
                "newPassword", "x",
                "nickname", "张三")});

        assertThat(json).doesNotContain("eyJhbGciOi").doesNotContain("2345").doesNotContain("s3cr3t")
                .contains("张三");
        assertThat(OperLogParams.isSensitive("accessToken")).isTrue();
        assertThat(OperLogParams.isSensitive("captchaId")).isTrue();
        assertThat(OperLogParams.isSensitive("code")).isFalse();
    }
}
