package com.platform.cloud.mall.starter;

import com.platform.cloud.bootstrap.common.security.JwtAuthenticationFilter;
import com.platform.cloud.bootstrap.common.security.JwtProperties;
import com.platform.cloud.bootstrap.common.security.JwtService;
import com.platform.cloud.bootstrap.common.security.TokenBlacklist;
import com.platform.cloud.bootstrap.common.security.TokenVersionChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * starter 过滤器：黑名单、版本号回调、Redis 异常时 access 令牌可用性优先。
 */
class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private JwtProperties props;
    private TokenBlacklist blacklist;
    private TokenVersionChecker checker;

    @BeforeEach
    void setUp() {
        props = new JwtProperties();
        props.setSecret("unit-test-secret-at-least-32-bytes-long!!");
        jwtService = new JwtService(props);
        jwtService.init();
        blacklist = mock(TokenBlacklist.class);
        checker = mock(TokenVersionChecker.class);
        when(checker.isCurrent(any())).thenReturn(true);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    private JwtAuthenticationFilter filter(TokenVersionChecker c) {
        ObjectProvider<TokenVersionChecker> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(c);
        return new JwtAuthenticationFilter(jwtService, props, blacklist, provider);
    }

    private MockHttpServletRequest request(String type) {
        String token = jwtService.generate("5", type, Map.of(JwtService.CLAIM_ROLES, List.of("ROLE_MEMBER")),
                Duration.ofMinutes(5));
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        return req;
    }

    private boolean authenticated(JwtAuthenticationFilter f, MockHttpServletRequest req) throws Exception {
        f.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    @Test
    void validTokenAuthenticatesAndExposesClaims() throws Exception {
        MockHttpServletRequest req = request(JwtService.TYPE_ACCESS);
        assertThat(authenticated(filter(checker), req)).isTrue();
        assertThat(req.getAttribute(JwtAuthenticationFilter.CLAIMS_ATTRIBUTE)).isNotNull();
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() throws Exception {
        assertThat(authenticated(filter(checker), request(JwtService.TYPE_REFRESH))).isFalse();
    }

    @Test
    void revokedTokenIsRejected() throws Exception {
        when(blacklist.isRevoked(anyString())).thenReturn(true);
        assertThat(authenticated(filter(checker), request(JwtService.TYPE_ACCESS))).isFalse();
    }

    @Test
    void staleVersionIsRejected() throws Exception {
        when(checker.isCurrent(any())).thenReturn(false);
        assertThat(authenticated(filter(checker), request(JwtService.TYPE_ACCESS))).isFalse();
    }

    @Test
    void noCheckerMeansNoVersionCheck() throws Exception {
        assertThat(authenticated(filter(null), request(JwtService.TYPE_ACCESS))).isTrue();
    }

    @Test
    void redisDownFailsOpenForAccessTokens() throws Exception {
        when(blacklist.isRevoked(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        when(checker.isCurrent(any())).thenThrow(new RedisConnectionFailureException("down"));
        assertThat(authenticated(filter(checker), request(JwtService.TYPE_ACCESS))).isTrue();
    }
}
