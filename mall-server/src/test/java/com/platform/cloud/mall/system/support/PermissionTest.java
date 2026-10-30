package com.platform.cloud.mall.system.support;

import com.platform.cloud.mall.common.security.PermissionService;
import com.platform.cloud.mall.system.entity.SysMenu;
import com.platform.cloud.mall.system.vo.MenuTreeVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 权限判断（@ss.has）与菜单树组装。
 */
class PermissionTest {

    private final PermissionService ss = new PermissionService();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminWithPermissionIsAllowed() {
        login("ROLE_ADMIN", "ROLE_operator", "goods:goods:view");
        assertThat(ss.has("goods:goods:view")).isTrue();
        assertThat(ss.has("goods:goods:edit")).isFalse();
        assertThat(ss.has("system:admin:view")).isFalse();
    }

    @Test
    void superAdminBypassesPermissionCodes() {
        login("ROLE_ADMIN", "ROLE_super_admin");
        assertThat(ss.has("system:admin:edit")).isTrue();
        assertThat(ss.has("anything:new")).isTrue();
    }

    @Test
    void memberTokenNeverPassesEvenWithForgedPermission() {
        login("ROLE_MEMBER", "goods:goods:view", "ROLE_super_admin");
        assertThat(ss.has("goods:goods:view")).isFalse();
    }

    @Test
    void anonymousIsDenied() {
        assertThat(ss.has("goods:goods:view")).isFalse();
    }

    @Test
    void menuTreeDropsOrphansAndSortsChildren() {
        List<MenuTreeVO> tree = MenuTreeBuilder.build(List.of(
                menu(2000L, 0L, "DIR", 2),
                menu(1000L, 0L, "DIR", 1),
                menu(2010L, 2000L, "MENU", 2),
                menu(2001L, 2000L, "MENU", 1),
                menu(9001L, 9000L, "MENU", 1)), false);

        assertThat(tree).extracting(MenuTreeVO::getId).containsExactly(1000L, 2000L);
        assertThat(tree.get(1).getChildren()).extracting(MenuTreeVO::getId).containsExactly(2001L, 2010L);
        assertThat(tree.get(1).getPermission()).isNull();
    }

    private static SysMenu menu(Long id, Long parentId, String type, int sort) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setParentId(parentId);
        m.setType(type);
        m.setName("m" + id);
        m.setSort(sort);
        m.setPermission("p:" + id);
        m.setStatus(1);
        return m;
    }

    private static void login(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("1", null,
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }
}
