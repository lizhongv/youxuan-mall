package com.platform.cloud.mall.system.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.common.security.TokenVersionStore;
import com.platform.cloud.mall.common.security.UserType;
import com.platform.cloud.mall.system.dto.AdminSaveRequest;
import com.platform.cloud.mall.system.entity.SysAdmin;
import com.platform.cloud.mall.system.entity.SysAdminRole;
import com.platform.cloud.mall.system.entity.SysRole;
import com.platform.cloud.mall.system.mapper.SysAdminMapper;
import com.platform.cloud.mall.system.mapper.SysAdminRoleMapper;
import com.platform.cloud.mall.system.mapper.SysRoleMapper;
import com.platform.cloud.mall.testsupport.TableInfoTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理员维护的防提权规则与令牌作废。
 */
class SysAdminServiceImplTest {

    private static final long SUPER_ROLE = 1L;
    private static final long OPERATOR_ROLE = 2L;

    private SysAdminMapper adminMapper;
    private SysAdminRoleMapper adminRoleMapper;
    private SysRoleMapper roleMapper;
    private TokenVersionStore versionStore;
    private SysAdminServiceImpl service;

    @BeforeAll
    static void initTables() {
        TableInfoTestSupport.init(SysAdmin.class, SysAdminRole.class, SysRole.class);
    }

    @BeforeEach
    void setUp() {
        adminMapper = mock(SysAdminMapper.class);
        adminRoleMapper = mock(SysAdminRoleMapper.class);
        roleMapper = mock(SysRoleMapper.class);
        versionStore = mock(TokenVersionStore.class);
        service = new SysAdminServiceImpl(adminMapper, adminRoleMapper, roleMapper, new BCryptPasswordEncoder(4),
                versionStore);
        SysRole superRole = new SysRole();
        superRole.setId(SUPER_ROLE);
        superRole.setCode("super_admin");
        when(roleMapper.selectOne(any())).thenReturn(superRole);
        when(roleMapper.selectBatchIds(anyCollection())).thenAnswer(inv -> ((java.util.Collection<?>) inv.getArgument(0))
                .stream().map(id -> {
                    SysRole r = new SysRole();
                    r.setId((Long) id);
                    return r;
                }).toList());
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(long id, String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                String.valueOf(id), null, Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    private static void loginAsOperator() {
        loginAs(99L, "ROLE_ADMIN", "ROLE_operator", "system:admin:edit");
    }

    private void target(long id, String username, Long... roleIds) {
        SysAdmin a = new SysAdmin();
        a.setId(id);
        a.setUsername(username);
        a.setStatus(1);
        when(adminMapper.selectById(id)).thenReturn(a);
        when(adminRoleMapper.selectList(any())).thenReturn(Arrays.stream(roleIds)
                .map(r -> new SysAdminRole(id, r)).toList());
    }

    private static AdminSaveRequest req(Long... roleIds) {
        AdminSaveRequest r = new AdminSaveRequest();
        r.setUsername("someone");
        r.setPassword("Passw0rd!");
        r.setRoleIds(roleIds.length == 0 ? null : List.of(roleIds));
        return r;
    }

    @Test
    void operatorCannotCreateSuperAdmin() {
        loginAsOperator();
        when(adminMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> service.create(req(SUPER_ROLE)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超级管理员角色");
        verify(adminMapper, never()).insert(any(SysAdmin.class));
    }

    @Test
    void createdAdminMustChangePassword() {
        loginAsOperator();
        when(adminMapper.selectCount(any())).thenReturn(0L);

        service.create(req(OPERATOR_ROLE));

        ArgumentCaptor<SysAdmin> saved = ArgumentCaptor.forClass(SysAdmin.class);
        verify(adminMapper).insert(saved.capture());
        assertThat(saved.getValue().getMustChangePassword()).isEqualTo(1);
    }

    @Test
    void operatorCannotGrantSuperRoleToExistingAdmin() {
        loginAsOperator();
        target(5L, "bob", OPERATOR_ROLE);

        assertThatThrownBy(() -> service.update(5L, req(OPERATOR_ROLE, SUPER_ROLE)))
                .hasMessageContaining("超级管理员角色");
    }

    @Test
    void operatorCannotModifySuperAdminAccount() {
        loginAsOperator();
        target(6L, "carol", SUPER_ROLE);

        assertThatThrownBy(() -> service.update(6L, req())).hasMessageContaining("无权操作超级管理员账号（修改）");
        assertThatThrownBy(() -> service.resetPassword(6L, "NewPass1")).hasMessageContaining("重置密码");
        assertThatThrownBy(() -> service.updateStatus(6L, 0)).hasMessageContaining("超级管理员");
        assertThatThrownBy(() -> service.delete(6L)).hasMessageContaining("超级管理员");
        verify(versionStore, never()).revokeAll(any(), any());
    }

    @Test
    void operatorCannotResetBuiltinAdminPassword() {
        loginAsOperator();
        target(1L, "admin");

        assertThatThrownBy(() -> service.resetPassword(1L, "NewPass1")).isInstanceOf(BusinessException.class);
    }

    @Test
    void superAdminCanResetOtherSuperAdminAndTokensAreRevoked() {
        loginAs(1L, "ROLE_ADMIN", "ROLE_super_admin");
        target(6L, "carol", SUPER_ROLE);

        assertThatCode(() -> service.resetPassword(6L, "NewPass1")).doesNotThrowAnyException();
        verify(versionStore).revokeAll(UserType.ADMIN, 6L);
    }

    @Test
    void operatorCanManageOrdinaryAdminAndRoleChangeRevokesTokens() {
        loginAsOperator();
        target(5L, "bob", OPERATOR_ROLE);

        service.update(5L, req(OPERATOR_ROLE, 3L));

        verify(versionStore).revokeAll(UserType.ADMIN, 5L);
    }

    @Test
    void unchangedRolesDoNotRevokeTokens() {
        loginAsOperator();
        target(5L, "bob", OPERATOR_ROLE);

        service.update(5L, req(OPERATOR_ROLE));

        verify(versionStore, never()).revokeAll(any(), any());
    }

    @Test
    void disablingAdminRevokesTokens() {
        loginAsOperator();
        target(5L, "bob", OPERATOR_ROLE);

        service.updateStatus(5L, 0);

        verify(versionStore).revokeAll(UserType.ADMIN, 5L);
    }

    @Test
    void operatorCannotRemoveSuperRoleFromSomeone() {
        loginAs(99L, "ROLE_ADMIN", "system:admin:edit");
        // 目标本身是超管：先被「无权修改超管账号」拦下
        target(6L, "carol", SUPER_ROLE);
        assertThatThrownBy(() -> service.update(6L, req(OPERATOR_ROLE))).isInstanceOf(BusinessException.class);
    }
}
