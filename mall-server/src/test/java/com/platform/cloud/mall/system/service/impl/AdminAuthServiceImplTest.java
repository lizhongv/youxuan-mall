package com.platform.cloud.mall.system.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.security.JwtProperties;
import com.platform.cloud.bootstrap.common.security.JwtService;
import com.platform.cloud.bootstrap.common.security.TokenBlacklist;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.mall.common.config.MallProperties;
import com.platform.cloud.mall.common.security.CaptchaService;
import com.platform.cloud.mall.common.security.LoginAttemptGuard;
import com.platform.cloud.mall.common.security.RedisCounter;
import com.platform.cloud.mall.common.security.TokenVersionStore;
import com.platform.cloud.mall.common.security.UserType;
import com.platform.cloud.mall.system.dto.AdminLoginRequest;
import com.platform.cloud.mall.system.dto.ChangePasswordRequest;
import com.platform.cloud.mall.system.entity.SysAdmin;
import com.platform.cloud.mall.system.entity.SysRole;
import com.platform.cloud.mall.system.mapper.SysAdminMapper;
import com.platform.cloud.mall.system.service.SysLoginLogService;
import com.platform.cloud.mall.system.service.SysMenuService;
import com.platform.cloud.mall.system.service.SysRoleService;
import com.platform.cloud.mall.system.vo.TokenVO;
import com.platform.cloud.mall.testsupport.TableInfoTestSupport;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理员登录：IP 限流、按「账号 + IP」失败锁定、失败 2 次后要求图形验证码、令牌版本号、refresh 一次性、登出。
 */
class AdminAuthServiceImplTest {

    /** BCrypt 计算较慢，测试用最低强度 */
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    /** 单测没有请求上下文，客户端 IP 为空串 */
    private static final String FAIL_KEY = "mall:login:fail:admin:admin:";

    private SysAdminMapper adminMapper;
    private SysRoleService roleService;
    private SysMenuService menuService;
    private SysLoginLogService loginLogService;
    private RedisCounter counter;
    private CaptchaService captchaService;
    private TokenBlacklist blacklist;
    private TokenVersionStore versionStore;
    private JwtService jwtService;
    private AdminAuthServiceImpl service;

    @BeforeAll
    static void initTables() {
        TableInfoTestSupport.init(SysAdmin.class);
    }

    @BeforeEach
    void setUp() {
        adminMapper = mock(SysAdminMapper.class);
        roleService = mock(SysRoleService.class);
        menuService = mock(SysMenuService.class);
        loginLogService = mock(SysLoginLogService.class);
        counter = mock(RedisCounter.class);
        captchaService = mock(CaptchaService.class);
        blacklist = mock(TokenBlacklist.class);
        versionStore = mock(TokenVersionStore.class);
        when(counter.incrementInWindow(anyString(), any())).thenReturn(1L);
        when(counter.incrementSliding(anyString(), any())).thenReturn(1L);

        JwtProperties props = new JwtProperties();
        props.setSecret("unit-test-secret-at-least-32-bytes-long!!");
        props.setAccessTtl(Duration.ofHours(2));
        jwtService = new JwtService(props);
        jwtService.init();

        LoginAttemptGuard guard = new LoginAttemptGuard(counter, captchaService, new MallProperties());
        service = new AdminAuthServiceImpl(adminMapper, roleService, menuService, loginLogService,
                guard, ENCODER, jwtService, props, blacklist, versionStore, new MallProperties());
    }

    @Test
    void lockedAccountIsRejectedBeforePasswordCheck() {
        when(counter.incrementSliding(eq(FAIL_KEY), any())).thenReturn(6L);

        assertThatThrownBy(() -> service.login(login("admin", "whatever")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已锁定");
        verify(adminMapper, never()).selectOne(any());
    }

    @Test
    void usernameCaseDoesNotBypassLock() {
        when(counter.incrementSliding(eq(FAIL_KEY), any())).thenReturn(6L);

        assertThatThrownBy(() -> service.login(login("ADMIN", "x"))).hasMessageContaining("已锁定");
    }

    @Test
    void ipRateLimitRejectsBurst() {
        when(counter.incrementInWindow(eq("mall:login:rate:admin:"), any())).thenReturn(21L);

        BusinessException e = catchThrowableOfType(() -> service.login(login("admin", "x")), BusinessException.class);
        assertThat(e.getCode()).isEqualTo(ResultCode.DUPLICATE_REQUEST.getCode());
        verify(adminMapper, never()).selectOne(any());
    }

    @Test
    void firstWrongPasswordDoesNotAskForCaptcha() {
        when(adminMapper.selectOne(any())).thenReturn(admin("right-password", 1));

        BusinessException e = catchThrowableOfType(() -> service.login(login("admin", "wrong")), BusinessException.class);

        assertThat(e).hasMessage("用户名或密码错误");
        assertThat(e.getData()).isEqualTo(Map.of("needCaptcha", false));
        verify(loginLogService).record(eq(UserType.ADMIN), eq(1L), eq("admin"), eq(false), anyString());
        verify(captchaService, never()).verify(anyString(), any(), any());
    }

    @Test
    void secondFailureTellsFrontendToShowCaptcha() {
        when(counter.incrementSliding(eq(FAIL_KEY), any())).thenReturn(2L);
        when(adminMapper.selectOne(any())).thenReturn(admin("right-password", 1));

        BusinessException e = catchThrowableOfType(() -> service.login(login("admin", "wrong")), BusinessException.class);

        assertThat(e.getData()).isEqualTo(Map.of("needCaptcha", true));
    }

    @Test
    void afterTwoFailuresLoginWithoutCaptchaIsRejectedAndNotCounted() {
        when(counter.incrementSliding(eq(FAIL_KEY), any())).thenReturn(3L);
        when(captchaService.verify(eq(CaptchaService.SCOPE_ADMIN), any(), any())).thenReturn(false);

        BusinessException e = catchThrowableOfType(() -> service.login(login("admin", "pwd")), BusinessException.class);

        assertThat(e.getCode()).isEqualTo(40002);
        assertThat(e.getData()).isEqualTo(Map.of("needCaptcha", true));
        verify(counter).decrement(FAIL_KEY);
        verify(adminMapper, never()).selectOne(any());
    }

    @Test
    void manyFailuresAcrossIpsRequireCaptchaEverywhere() {
        when(counter.get("mall:login:fail:admin:admin")).thenReturn(10L);
        when(captchaService.verify(any(), any(), any())).thenReturn(false);

        BusinessException e = catchThrowableOfType(() -> service.login(login("admin", "pwd")), BusinessException.class);
        assertThat(e.getCode()).isEqualTo(40002);
    }

    @Test
    void fifthFailureLocksThisIp() {
        when(counter.incrementSliding(eq(FAIL_KEY), any())).thenReturn(5L);
        when(captchaService.verify(any(), any(), any())).thenReturn(true);
        when(adminMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.login(login("ghost", "x")))
                .isInstanceOf(BusinessException.class);
        when(counter.incrementSliding(eq("mall:login:fail:admin:ghost:"), any())).thenReturn(5L);
        assertThatThrownBy(() -> service.login(login("ghost", "x"))).hasMessageContaining("锁定 15 分钟");
    }

    @Test
    void disabledAdminCannotLoginAndAttemptIsReleased() {
        when(adminMapper.selectOne(any())).thenReturn(admin("pwd123", 0));

        assertThatThrownBy(() -> service.login(login("admin", "pwd123"))).hasMessageContaining("停用");
        verify(counter).decrement(FAIL_KEY);
    }

    @Test
    void successfulLoginResetsCountersAndIssuesTokensWithRolesPermissionsAndVersion() {
        SysAdmin a = admin("pwd123", 1);
        a.setTokenVersion(3);
        when(adminMapper.selectOne(any())).thenReturn(a);
        SysRole role = new SysRole();
        role.setId(9L);
        role.setCode("operator");
        when(roleService.enabledRolesOfAdmin(1L)).thenReturn(List.of(role));
        when(menuService.permissionsOf(anyCollection(), anyBoolean()))
                .thenReturn(List.of("goods:goods:edit", "goods:goods:view"));

        TokenVO token = service.login(login("admin", "pwd123"));

        verify(counter).delete(FAIL_KEY);
        verify(counter).delete("mall:login:fail:admin:admin");
        verify(adminMapper).update(isNull(), any());
        assertThat(token.getExpiresIn()).isEqualTo(7200);

        Claims access = jwtService.parse(token.getAccessToken()).getPayload();
        assertThat(access.getSubject()).isEqualTo("1");
        assertThat(access.get(JwtService.CLAIM_TYPE)).isEqualTo(JwtService.TYPE_ACCESS);
        assertThat(access.get(JwtService.CLAIM_ROLES, List.class))
                .containsExactly("ROLE_ADMIN", "ROLE_operator", "goods:goods:edit", "goods:goods:view");
        assertThat(access.get(JwtService.CLAIM_TOKEN_VERSION)).isEqualTo(3);
        assertThat(access.get("mcp")).isNull();

        Claims refresh = jwtService.parse(token.getRefreshToken()).getPayload();
        assertThat(refresh.get(JwtService.CLAIM_TYPE)).isEqualTo(JwtService.TYPE_REFRESH);
        assertThat(refresh.get("utype")).isEqualTo("ADMIN");
        assertThat(refresh.get(JwtService.CLAIM_TOKEN_VERSION)).isEqualTo(3);
        assertThat(refresh.get(JwtService.CLAIM_ROLES)).isNull();
    }

    @Test
    void initialPasswordAdminGetsMustChangeClaim() {
        SysAdmin a = admin("pwd123", 1);
        a.setMustChangePassword(1);
        when(adminMapper.selectOne(any())).thenReturn(a);

        TokenVO token = service.login(login("admin", "pwd123"));

        assertThat(jwtService.parse(token.getAccessToken()).getPayload().get("mcp")).isEqualTo(true);
    }

    @Test
    void meReportsMustChangePassword() {
        SysAdmin a = admin("pwd123", 1);
        a.setMustChangePassword(1);
        when(adminMapper.selectById(1L)).thenReturn(a);

        assertThat(service.me(1L).getMustChangePassword()).isTrue();
    }

    @Test
    void refreshRejectsMemberToken() {
        String memberRefresh = jwtService.generate("1", JwtService.TYPE_REFRESH,
                Map.of("utype", "MEMBER"), Duration.ofDays(1));

        assertThatThrownBy(() -> service.refresh(memberRefresh))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重新登录");
    }

    @Test
    void refreshRejectsAccessToken() {
        String access = jwtService.generate("1", JwtService.TYPE_ACCESS,
                Map.of("utype", "ADMIN"), Duration.ofHours(1));

        assertThatThrownBy(() -> service.refresh(access)).isInstanceOf(BusinessException.class);
    }

    @Test
    void refreshTokenCanOnlyBeUsedOnce() {
        String refresh = adminRefresh();
        when(adminMapper.selectById(1L)).thenReturn(admin("pwd", 1));
        when(versionStore.matches(eq(UserType.ADMIN), eq(1L), any())).thenReturn(true);
        when(blacklist.tryConsume(anyString(), any())).thenReturn(true, false);

        assertThat(service.refresh(refresh).getAccessToken()).isNotBlank();
        assertThatThrownBy(() -> service.refresh(refresh)).hasMessageContaining("重新登录");
    }

    @Test
    void refreshRejectsTokenIssuedBeforePasswordChange() {
        String refresh = adminRefresh();
        when(adminMapper.selectById(1L)).thenReturn(admin("pwd", 1));
        when(versionStore.matches(eq(UserType.ADMIN), eq(1L), any())).thenReturn(false);

        assertThatThrownBy(() -> service.refresh(refresh)).hasMessageContaining("失效");
        verify(blacklist, never()).tryConsume(anyString(), any());
    }

    @Test
    void logoutRevokesAccessAndRefresh() {
        String access = jwtService.generate("1", JwtService.TYPE_ACCESS, Map.of("utype", "ADMIN"), Duration.ofHours(1));
        String refresh = adminRefresh();
        String accessJti = jwtService.parse(access).getPayload().getId();
        String refreshJti = jwtService.parse(refresh).getPayload().getId();

        service.logout(access, refresh);

        verify(blacklist).revoke(eq(accessJti), any());
        verify(blacklist).revoke(eq(refreshJti), any());
    }

    @Test
    void logoutIgnoresMemberRefreshToken() {
        String memberRefresh = jwtService.generate("1", JwtService.TYPE_REFRESH, Map.of("utype", "MEMBER"),
                Duration.ofDays(1));

        service.logout(null, memberRefresh);

        verify(blacklist, never()).revoke(anyString(), any());
    }

    @Test
    void changePasswordRevokesOldTokensAndIssuesNewOnes() {
        SysAdmin a = admin("old-pwd", 1);
        a.setMustChangePassword(1);
        SysAdmin after = admin("new-pwd", 1);
        after.setTokenVersion(1);
        when(adminMapper.selectById(1L)).thenReturn(a, after);
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old-pwd");
        req.setNewPassword("new-pwd");

        TokenVO token = service.changePassword(1L, req);

        verify(versionStore).revokeAll(UserType.ADMIN, 1L);
        Claims claims = jwtService.parse(token.getAccessToken()).getPayload();
        assertThat(claims.get(JwtService.CLAIM_TOKEN_VERSION)).isEqualTo(1);
        assertThat(claims.get("mcp")).isNull();
    }

    @Test
    void changePasswordRequiresOldPassword() {
        when(adminMapper.selectById(1L)).thenReturn(admin("old-pwd", 1));
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("wrong");
        req.setNewPassword("new-pwd");

        assertThatThrownBy(() -> service.changePassword(1L, req)).hasMessage("原密码错误");
        verify(versionStore, never()).revokeAll(any(), any());
    }

    private String adminRefresh() {
        return jwtService.generate("1", JwtService.TYPE_REFRESH, Map.of("utype", "ADMIN", "tv", 0), Duration.ofDays(1));
    }

    private static AdminLoginRequest login(String username, String password) {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }

    private static SysAdmin admin(String rawPassword, int status) {
        SysAdmin a = new SysAdmin();
        a.setId(1L);
        a.setUsername("admin");
        a.setPassword(ENCODER.encode(rawPassword));
        a.setStatus(status);
        return a;
    }
}
