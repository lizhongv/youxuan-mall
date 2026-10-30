package com.platform.cloud.mall.system.service;

import com.platform.cloud.mall.system.dto.MenuSaveRequest;
import com.platform.cloud.mall.system.vo.MenuTreeVO;

import java.util.Collection;
import java.util.List;

/**
 * 菜单树维护，以及按角色计算权限码与可见菜单。
 */
public interface SysMenuService {

    /** 全部菜单树（含按钮、含停用），菜单管理与角色授权页使用 */
    List<MenuTreeVO> tree();

    MenuTreeVO detail(Long id);

    Long create(MenuSaveRequest req);

    void update(Long id, MenuSaveRequest req);

    void delete(Long id);

    /** 角色集合拥有的权限码；超管拥有全部启用菜单的权限码 */
    List<String> permissionsOf(Collection<Long> roleIds, boolean superAdmin);

    /** 角色集合可见的目录 / 菜单树（不含按钮） */
    List<MenuTreeVO> menuTreeOf(Collection<Long> roleIds, boolean superAdmin);
}
