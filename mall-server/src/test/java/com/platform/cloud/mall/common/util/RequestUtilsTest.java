package com.platform.cloud.mall.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 客户端 IP：X-Forwarded-For 不可信；X-Real-IP 只在请求来自内网代理（Nginx）时可信。
 */
class RequestUtilsTest {

    @Test
    void forwardedForIsIgnored() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("172.18.0.5");
        req.addHeader("X-Forwarded-For", "1.1.1.1, 172.18.0.1");
        req.addHeader("X-Real-IP", "203.0.113.9");

        assertThat(RequestUtils.clientIp(req)).isEqualTo("203.0.113.9");
    }

    @Test
    void realIpFromNginxIsTrusted() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("127.0.0.1");
        req.addHeader("X-Real-IP", "198.51.100.7");

        assertThat(RequestUtils.clientIp(req)).isEqualTo("198.51.100.7");
    }

    @Test
    void realIpFromPublicClientIsIgnored() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("203.0.113.50");
        req.addHeader("X-Real-IP", "10.0.0.1");
        req.addHeader("X-Forwarded-For", "8.8.8.8");

        assertThat(RequestUtils.clientIp(req)).isEqualTo("203.0.113.50");
    }

    @Test
    void fallsBackToRemoteAddrWithoutHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("192.168.1.20");

        assertThat(RequestUtils.clientIp(req)).isEqualTo("192.168.1.20");
    }

    @Test
    void trustedProxyRanges() {
        assertThat(RequestUtils.isTrustedProxy("10.1.2.3")).isTrue();
        assertThat(RequestUtils.isTrustedProxy("172.16.0.1")).isTrue();
        assertThat(RequestUtils.isTrustedProxy("::1")).isTrue();
        assertThat(RequestUtils.isTrustedProxy("fd00::1")).isTrue();
        assertThat(RequestUtils.isTrustedProxy("8.8.8.8")).isFalse();
        // 非 IP 字面量不做 DNS 解析，直接不信任
        assertThat(RequestUtils.isTrustedProxy("localhost")).isFalse();
        assertThat(RequestUtils.isTrustedProxy(null)).isFalse();
    }

    @Test
    void overlongValueIsTruncated() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("127.0.0.1");
        req.addHeader("X-Real-IP", "x".repeat(100));

        assertThat(RequestUtils.clientIp(req)).hasSize(64);
    }
}
