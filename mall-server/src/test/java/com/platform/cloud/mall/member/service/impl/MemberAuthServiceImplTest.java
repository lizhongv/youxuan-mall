package com.platform.cloud.mall.member.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.security.JwtProperties;
import com.platform.cloud.bootstrap.common.security.JwtService;
import com.platform.cloud.bootstrap.common.security.TokenBlacklist;
import com.platform.cloud.mall.common.config.MallProperties;
import com.platform.cloud.mall.common.security.CaptchaService;
import com.platform.cloud.mall.common.security.LoginAttemptGuard;
import com.platform.cloud.mall.common.security.RedisCounter;
import com.platform.cloud.mall.common.security.TokenVersionStore;
import com.platform.cloud.mall.common.security.UserType;
import com.platform.cloud.mall.member.dto.MemberLoginRequest;
import com.platform.cloud.mall.member.dto.MemberResetPasswordRequest;
import com.platform.cloud.mall.member.entity.MbrMember;
import com.platform.cloud.mall.member.mapper.MbrMemberMapper;
import com.platform.cloud.mall.member.service.MemberAccountService;
import com.platform.cloud.mall.member.service.MemberLevelService;
import com.platform.cloud.mall.member.service.SmsCodeService;
import com.platform.cloud.mall.member.vo.MemberTokenVO;
import com.platform.cloud.mall.system.api.ConfigApi;
import com.platform.cloud.mall.system.service.SysLoginLogService;
import com.platform.cloud.mall.testsupport.TableInfoTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会员认证：令牌版本号、refresh 一次性、重置密码作废旧令牌、登出拉黑 refresh、冻结账号。
 */
class MemberAuthServiceImplTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    private MbrMemberMapper memberMapper;
    private RedisCounter counter;
    private TokenBlacklist blacklist;
    private TokenVersionStore versionStore;
    private JwtService jwtService;
    private MemberAuthServiceImpl service;

    @BeforeAll
    static void initTables() {
        TableInfoTestSupport.init(MbrMember.class);
    }

    @BeforeEach
    void setUp() {
        memberMapper = mock(MbrMemberMapper.class);
        counter = mock(RedisCounter.class);
        when(counter.incrementInWindow(anyString(), any())).thenReturn(1L);
        when(counter.incrementSliding(anyString(), any())).thenReturn(1L);
        blacklist = mock(TokenBlacklist.class);
        versionStore = mock(TokenVersionStore.class);
        JwtProperties props = new JwtProperties();
        props.setSecret("unit-test-secret-at-least-32-bytes-long!!");
        jwtService = new JwtService(props);
        jwtService.init();
        service = new MemberAuthServiceImpl(memberMapper, mock(MemberLevelService.class),
                mock(MemberAccountService.class), mock(SmsCodeService.class),
                new LoginAttemptGuard(counter, mock(CaptchaService.class), new MallProperties()),
                mock(ConfigApi.class), mock(SysLoginLogService.class), ENCODER, jwtService, props, blacklist,
                versionStore);
    }

    private static MbrMember member(int status, int tv) {
        MbrMember m = new MbrMember();
        m.setId(7L);
        m.setMobile("13800001234");
        m.setPassword(ENCODER.encode("Pass1234"));
        m.setStatus(status);
        m.setTokenVersion(tv);
        return m;
    }

    @Test
    void issuedTokensCarryVersion() {
        when(memberMapper.selectById(7L)).thenReturn(member(1, 4));

        MemberTokenVO t = service.issueTokens(7L);

        assertThat(jwtService.parse(t.getAccessToken()).getPayload().get(JwtService.CLAIM_TOKEN_VERSION)).isEqualTo(4);
        assertThat(jwtService.parse(t.getRefreshToken()).getPayload().get(JwtService.CLAIM_TOKEN_VERSION)).isEqualTo(4);
    }

    @Test
    void refreshIsOneTimeAndChecksVersion() {
        String refresh = jwtService.generate("7", JwtService.TYPE_REFRESH, Map.of("utype", "MEMBER", "tv", 0),
                Duration.ofDays(1));
        when(memberMapper.selectById(7L)).thenReturn(member(1, 0));
        when(versionStore.matches(eq(UserType.MEMBER), eq(7L), any())).thenReturn(true);
        when(blacklist.tryConsume(anyString(), any())).thenReturn(true, false);

        assertThat(service.refresh(refresh).getAccessToken()).isNotBlank();
        assertThatThrownBy(() -> service.refresh(refresh)).isInstanceOf(BusinessException.class);

        when(versionStore.matches(eq(UserType.MEMBER), eq(7L), any())).thenReturn(false);
        assertThatThrownBy(() -> service.refresh(refresh)).hasMessageContaining("失效");
    }

    @Test
    void frozenMemberCannotRefresh() {
        String refresh = jwtService.generate("7", JwtService.TYPE_REFRESH, Map.of("utype", "MEMBER"), Duration.ofDays(1));
        when(memberMapper.selectById(7L)).thenReturn(member(0, 0));

        assertThatThrownBy(() -> service.refresh(refresh)).hasMessageContaining("冻结");
    }

    @Test
    void frozenMemberPasswordLoginIsRejectedWithoutCountingFailure() {
        when(memberMapper.selectOne(any())).thenReturn(member(0, 0));
        MemberLoginRequest req = new MemberLoginRequest();
        req.setMobile("13800001234");
        req.setPassword("Pass1234");

        assertThatThrownBy(() -> service.login(req)).hasMessageContaining("冻结");
        verify(counter).decrement("mall:login:fail:member:13800001234:");
    }

    @Test
    void resetPasswordRevokesAllTokens() {
        when(memberMapper.selectOne(any())).thenReturn(member(1, 0));
        MemberResetPasswordRequest req = new MemberResetPasswordRequest();
        req.setMobile("13800001234");
        req.setCode("123456");
        req.setPassword("NewPass123");

        service.resetPassword(req);

        verify(versionStore).revokeAll(UserType.MEMBER, 7L);
    }

    @Test
    void logoutRevokesRefreshToo() {
        String access = jwtService.generate("7", JwtService.TYPE_ACCESS, Map.of("utype", "MEMBER"), Duration.ofHours(1));
        String refresh = jwtService.generate("7", JwtService.TYPE_REFRESH, Map.of("utype", "MEMBER"), Duration.ofDays(1));

        service.logout(access, refresh);

        verify(blacklist).revoke(eq(jwtService.parse(access).getPayload().getId()), any());
        verify(blacklist).revoke(eq(jwtService.parse(refresh).getPayload().getId()), any());
    }
}
