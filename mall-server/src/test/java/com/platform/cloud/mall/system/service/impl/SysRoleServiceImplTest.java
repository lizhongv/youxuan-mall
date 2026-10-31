package com.platform.cloud.mall.system.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.system.entity.SysAdminRole;
import com.platform.cloud.mall.system.entity.SysRole;
import com.platform.cloud.mall.system.entity.SysRoleMenu;
import com.platform.cloud.mall.system.mapper.SysAdminRoleMapper;
import com.platform.cloud.mall.system.mapper.SysMenuMapper;
import com.platform.cloud.mall.system.mapper.SysRoleMapper;
import com.platform.cloud.mall.system.mapper.SysRoleMenuMapper;
import com.platform.cloud.mall.system.support.SystemConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 非超管给角色授权只能在自己拥有的菜单范围内增减，防止提权。
 */
@ExtendWith(MockitoExtension.class)
class SysRoleServiceImplTest {

    private static final long ME = 9L;
    private static final long MY_ROLE = 2L;
    private static final long TARGET_ROLE = 3L;

    @Mock
    SysRoleMapper roleMapper;
    @Mock
    SysRoleMenuMapper roleMenuMapper;
    @Mock
    SysAdminRoleMapper adminRoleMapper;
    @Mock
    SysMenuMapper menuMapper;
    @InjectMocks
    SysRoleServiceImpl service;

    @BeforeEach
    void loginAsOperator() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                String.valueOf(ME), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static SysRole role(long id, String code) {
        SysRole r = new SysRole();
        r.setId(id);
        r.setCode(code);
        return r;
    }

    private void myMenus(Long... menuIds) {
        SysAdminRole ar = new SysAdminRole();
        ar.setAdminId(ME);
        ar.setRoleId(MY_ROLE);
        when(adminRoleMapper.selectList(any())).thenReturn(List.of(ar));
        List<SysRoleMenu> own = java.util.Arrays.stream(menuIds).map(m -> new SysRoleMenu(MY_ROLE, m)).toList();
        when(roleMenuMapper.selectList(any())).thenReturn(own);
    }

    @Test
    void cannotGrantMenuOperatorDoesNotHave() {
        myMenus(10L, 11L);
        assertThatThrownBy(() -> service.restrictToOwnMenus(role(TARGET_ROLE, "clerk"), Set.of(10L, 99L)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("没有的权限");
    }

    @Test
    void keepsExistingMenusOutsideOperatorScope() {
        myMenus(10L, 11L);
        // 第二次 selectList 读取目标角色现有菜单：10 和 运营没有的 50
        when(roleMenuMapper.selectList(any()))
                .thenReturn(List.of(new SysRoleMenu(MY_ROLE, 10L), new SysRoleMenu(MY_ROLE, 11L)))
                .thenReturn(List.of(new SysRoleMenu(TARGET_ROLE, 10L), new SysRoleMenu(TARGET_ROLE, 50L)));

        Set<Long> result = service.restrictToOwnMenus(role(TARGET_ROLE, "clerk"), new LinkedHashSet<>(List.of(11L)));

        assertThat(result).containsExactlyInAnyOrder(50L, 11L);
    }

    @Test
    void cannotEditOwnRole() {
        SysAdminRole ar = new SysAdminRole();
        ar.setAdminId(ME);
        ar.setRoleId(MY_ROLE);
        when(adminRoleMapper.selectList(any())).thenReturn(List.of(ar));
        assertThatThrownBy(() -> service.restrictToOwnMenus(role(MY_ROLE, "operator"), Set.of(10L)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己所属角色");
    }

    @Test
    void cannotEditSuperAdminRole() {
        assertThatThrownBy(() -> service.restrictToOwnMenus(role(1L, SystemConstants.SUPER_ADMIN_CODE), Set.of()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("超级管理员");
    }
}
