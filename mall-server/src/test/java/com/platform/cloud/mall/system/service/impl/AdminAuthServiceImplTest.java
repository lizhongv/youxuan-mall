package com.platform.cloud.mall.system.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.security.JwtProperties;
import com.platform.cloud.bootstrap.common.security.JwtService;
import com.platform.cloud.bootstrap.common.security.TokenBlacklist;
import com.platform.cloud.mall.common.security.UserType;
import com.platform.cloud.mall.system.dto.AdminLoginRequest;
import com.platform.cloud.mall.system.entity.SysAdmin;
import com.platform.cloud.mall.system.entity.SysRole;
import com.platform.cloud.mall.system.mapper.SysAdminMapper;
import com.platform.cloud.mall.system.service.SysLoginLogService;
import com.platform.cloud.mall.system.service.SysMenuService;
import com.platform.cloud.mall.system.service.SysRoleService;
import com.platform.cloud.mall.system.support.LoginFailureGuard;
import com.platform.cloud.mall.system.vo.TokenVO;
import com.platform.cloud.mall.testsupport.TableInfoTestSupport;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
 * 管理员登录：失败计数、锁定、令牌内容。
 */
class AdminAuthServiceImplTest {

    /** BCrypt 计算较慢，测试用最低强度 */
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    private SysAdminMapper adminMapper;
    private SysRoleService roleService;
    private SysMenuService menuService;
    private SysLoginLogService loginLogService;
    private ValueOperations<String, String> ops;
    private StringRedisTemplate redis;
    private JwtService jwtService;
    private AdminAuthServiceImpl service;

    @BeforeAll
    static void initTables() {
        TableInfoTestSupport.init(SysAdmin.class);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        adminMapper = mock(SysAdminMapper.class);
        roleService = mock(SysRoleService.class);
        menuService = mock(SysMenuService.class);
        loginLogService = mock(SysLoginLogService.class);
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);

        JwtProperties props = new JwtProperties();
        props.setSecret("unit-test-secret-at-least-32-bytes-long!!");
        props.setAccessTtl(Duration.ofHours(2));
        jwtService = new JwtService(props);
        jwtService.init();

        service = new AdminAuthServiceImpl(adminMapper, roleService, menuService, loginLogService,
                new LoginFailureGuard(redis), ENCODER, jwtService, props, mock(TokenBlacklist.class));
    }

    @Test
    void lockedAccountIsRejectedBeforePasswordCheck() {
        when(ops.get("mall:login:fail:admin:admin")).thenReturn("5");

        assertThatThrownBy(() -> service.login(login("admin", "whatever")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已锁定");
        verify(adminMapper, never()).selectOne(any());
    }

    @Test
    void wrongPasswordIncrementsFailureCounter() {
        when(adminMapper.selectOne(any())).thenReturn(admin("right-password", 1));
        when(ops.increment("mall:login:fail:admin:admin")).thenReturn(1L);

        assertThatThrownBy(() -> service.login(login("admin", "wrong")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
        verify(redis).expire("mall:login:fail:admin:admin", LoginFailureGuard.LOCK_DURATION);
        verify(loginLogService).record(eq(UserType.ADMIN), eq(1L), eq("admin"), eq(false), anyString());
    }

    @Test
    void fifthFailureLocksAccount() {
        when(adminMapper.selectOne(any())).thenReturn(null);
        when(ops.increment("mall:login:fail:admin:ghost")).thenReturn(5L);

        assertThatThrownBy(() -> service.login(login("ghost", "x")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("锁定 15 分钟");
    }

    @Test
    void usernameCaseDoesNotBypassLock() {
        when(ops.get("mall:login:fail:admin:admin")).thenReturn("5");

        assertThatThrownBy(() -> service.login(login("ADMIN", "x")))
                .hasMessageContaining("已锁定");
    }

    @Test
    void disabledAdminCannotLogin() {
        when(adminMapper.selectOne(any())).thenReturn(admin("pwd123", 0));

        assertThatThrownBy(() -> service.login(login("admin", "pwd123")))
                .hasMessageContaining("停用");
    }

    @Test
    void successfulLoginResetsCounterAndIssuesTokensWithRolesAndPermissions() {
        when(adminMapper.selectOne(any())).thenReturn(admin("pwd123", 1));
        SysRole role = new SysRole();
        role.setId(9L);
        role.setCode("operator");
        when(roleService.enabledRolesOfAdmin(1L)).thenReturn(List.of(role));
        when(menuService.permissionsOf(anyCollection(), anyBoolean()))
                .thenReturn(List.of("goods:goods:edit", "goods:goods:view"));

        TokenVO token = service.login(login("admin", "pwd123"));

        verify(redis).delete("mall:login:fail:admin:admin");
        verify(adminMapper).update(isNull(), any());
        assertThat(token.getExpiresIn()).isEqualTo(7200);

        Claims access = jwtService.parse(token.getAccessToken()).getPayload();
        assertThat(access.getSubject()).isEqualTo("1");
        assertThat(access.get(JwtService.CLAIM_TYPE)).isEqualTo(JwtService.TYPE_ACCESS);
        assertThat(access.get(JwtService.CLAIM_ROLES, List.class))
                .containsExactly("ROLE_ADMIN", "ROLE_operator", "goods:goods:edit", "goods:goods:view");

        Claims refresh = jwtService.parse(token.getRefreshToken()).getPayload();
        assertThat(refresh.get(JwtService.CLAIM_TYPE)).isEqualTo(JwtService.TYPE_REFRESH);
        assertThat(refresh.get("utype")).isEqualTo("ADMIN");
        assertThat(refresh.get(JwtService.CLAIM_ROLES)).isNull();
    }

    @Test
    void refreshRejectsMemberToken() {
        String memberRefresh = jwtService.generate("1", JwtService.TYPE_REFRESH,
                java.util.Map.of("utype", "MEMBER"), Duration.ofDays(1));

        assertThatThrownBy(() -> service.refresh(memberRefresh))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重新登录");
    }

    @Test
    void refreshRejectsAccessToken() {
        String access = jwtService.generate("1", JwtService.TYPE_ACCESS,
                java.util.Map.of("utype", "ADMIN"), Duration.ofHours(1));

        assertThatThrownBy(() -> service.refresh(access)).isInstanceOf(BusinessException.class);
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
