package com.platform.cloud.mall.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.system.dto.MenuSaveRequest;
import com.platform.cloud.mall.system.entity.SysMenu;
import com.platform.cloud.mall.system.entity.SysRoleMenu;
import com.platform.cloud.mall.system.mapper.SysMenuMapper;
import com.platform.cloud.mall.system.mapper.SysRoleMenuMapper;
import com.platform.cloud.mall.system.service.SysMenuService;
import com.platform.cloud.mall.system.support.MenuTreeBuilder;
import com.platform.cloud.mall.system.support.SystemConstants;
import com.platform.cloud.mall.system.vo.MenuTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜单维护与权限计算。菜单表只有几十行，全部读出来在内存里算，比多次关联查询简单可靠。
 */
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public List<MenuTreeVO> tree() {
        return MenuTreeBuilder.build(menuMapper.selectList(null), true);
    }

    @Override
    public MenuTreeVO detail(Long id) {
        return MenuTreeBuilder.toNode(require(id), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MenuSaveRequest req) {
        long parentId = req.getParentId() == null ? 0L : req.getParentId();
        validate(req, parentId, null);
        SysMenu m = new SysMenu();
        m.setParentId(parentId);
        apply(m, req);
        menuMapper.insert(m);
        return m.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MenuSaveRequest req) {
        require(id);
        long parentId = req.getParentId() == null ? 0L : req.getParentId();
        validate(req, parentId, id);
        SysMenu m = new SysMenu();
        m.setParentId(parentId);
        apply(m, req);
        menuMapper.update(null, new LambdaUpdateWrapper<SysMenu>()
                .set(SysMenu::getParentId, m.getParentId())
                .set(SysMenu::getType, m.getType())
                .set(SysMenu::getName, m.getName())
                .set(SysMenu::getPath, m.getPath())
                .set(SysMenu::getComponent, m.getComponent())
                .set(SysMenu::getIcon, m.getIcon())
                .set(SysMenu::getPermission, m.getPermission())
                .set(SysMenu::getSort, m.getSort())
                .set(SysMenu::getVisible, m.getVisible())
                .set(SysMenu::getStatus, m.getStatus())
                .eq(SysMenu::getId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        require(id);
        Long children = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (children != null && children > 0) {
            throw new BusinessException("请先删除子菜单");
        }
        menuMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));
    }

    @Override
    public List<String> permissionsOf(Collection<Long> roleIds, boolean superAdmin) {
        List<SysMenu> menus = superAdmin ? enabledMenus() : grantedMenus(roleIds);
        // TreeSet 去重并排序，令牌内容稳定，便于排查
        return menus.stream()
                .map(SysMenu::getPermission)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(TreeSet::new))
                .stream().toList();
    }

    @Override
    public List<MenuTreeVO> menuTreeOf(Collection<Long> roleIds, boolean superAdmin) {
        List<SysMenu> all = enabledMenus();
        Set<Long> visibleIds;
        if (superAdmin) {
            visibleIds = all.stream().map(SysMenu::getId).collect(Collectors.toSet());
        } else {
            // 授权时前端可能只提交叶子节点，这里补上祖先目录，保证菜单能挂到树上
            Map<Long, SysMenu> byId = all.stream().collect(Collectors.toMap(SysMenu::getId, Function.identity()));
            visibleIds = new HashSet<>();
            for (SysMenu m : grantedMenus(roleIds)) {
                SysMenu cur = m;
                while (cur != null && visibleIds.add(cur.getId())) {
                    cur = byId.get(cur.getParentId());
                }
            }
        }
        List<SysMenu> menus = all.stream()
                .filter(m -> visibleIds.contains(m.getId()))
                .filter(m -> !SysMenu.TYPE_BUTTON.equals(m.getType()))
                .toList();
        return MenuTreeBuilder.build(menus, false);
    }

    private List<SysMenu> enabledMenus() {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getStatus, SystemConstants.ENABLED));
    }

    private List<SysMenu> grantedMenus(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        Set<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds))
                .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getStatus, SystemConstants.ENABLED));
    }

    private void validate(MenuSaveRequest req, long parentId, Long selfId) {
        if (SysMenu.TYPE_BUTTON.equals(req.getType()) && !StringUtils.hasText(req.getPermission())) {
            throw new BusinessException("按钮必须填写权限码");
        }
        if (parentId == 0L) {
            if (SysMenu.TYPE_BUTTON.equals(req.getType())) {
                throw new BusinessException("按钮必须挂在菜单下");
            }
            return;
        }
        if (selfId != null && selfId == parentId) {
            throw new BusinessException("上级菜单不能是自己");
        }
        SysMenu parent = menuMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException("上级菜单不存在");
        }
        if (SysMenu.TYPE_BUTTON.equals(parent.getType())) {
            throw new BusinessException("按钮下不能再挂子节点");
        }
        if (selfId != null) {
            // 防止把节点移到自己的子孙下面形成环，环会让树构建时整枝消失
            Map<Long, Long> parentOf = menuMapper.selectList(null).stream()
                    .collect(Collectors.toMap(SysMenu::getId, SysMenu::getParentId));
            Long cur = parentId;
            Set<Long> seen = new HashSet<>();
            while (cur != null && cur != 0L && seen.add(cur)) {
                if (cur.equals(selfId)) {
                    throw new BusinessException("不能移动到自己的下级菜单");
                }
                cur = parentOf.get(cur);
            }
        }
    }

    private static void apply(SysMenu m, MenuSaveRequest req) {
        m.setType(req.getType());
        m.setName(req.getName().trim());
        m.setPath(Objects.toString(req.getPath(), ""));
        m.setComponent(Objects.toString(req.getComponent(), ""));
        m.setIcon(Objects.toString(req.getIcon(), ""));
        m.setPermission(Objects.toString(req.getPermission(), ""));
        m.setSort(req.getSort() == null ? 0 : req.getSort());
        m.setVisible(req.getVisible() == null ? SystemConstants.ENABLED : req.getVisible());
        m.setStatus(req.getStatus() == null ? SystemConstants.ENABLED : req.getStatus());
    }

    private SysMenu require(Long id) {
        SysMenu m = menuMapper.selectById(id);
        if (m == null) {
            throw new BusinessException("菜单不存在");
        }
        return m;
    }
}
