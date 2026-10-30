package com.platform.cloud.mall.system.support;

import com.platform.cloud.mall.system.entity.SysMenu;
import com.platform.cloud.mall.system.vo.MenuTreeVO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把扁平菜单组装成树。父节点不在集合里的节点（父被停用或未授权）直接丢弃，避免出现无处挂载的孤儿菜单。
 */
public final class MenuTreeBuilder {

    private static final Comparator<MenuTreeVO> ORDER = Comparator
            .comparing((MenuTreeVO m) -> m.getSort() == null ? 0 : m.getSort())
            .thenComparing(MenuTreeVO::getId);

    private MenuTreeBuilder() {
    }

    public static List<MenuTreeVO> build(List<SysMenu> menus, boolean withAdminFields) {
        Map<Long, MenuTreeVO> nodes = new LinkedHashMap<>();
        for (SysMenu m : menus) {
            nodes.put(m.getId(), toNode(m, withAdminFields));
        }
        List<MenuTreeVO> roots = new ArrayList<>();
        for (MenuTreeVO node : nodes.values()) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0L) {
                roots.add(node);
            } else {
                MenuTreeVO parent = nodes.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        sort(roots);
        return roots;
    }

    private static void sort(List<MenuTreeVO> list) {
        list.sort(ORDER);
        for (MenuTreeVO n : list) {
            sort(n.getChildren());
        }
    }

    /** 单个节点映射；withAdminFields 为 false 时不输出 permission / status */
    public static MenuTreeVO toNode(SysMenu m, boolean withAdminFields) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setId(m.getId());
        vo.setParentId(m.getParentId());
        vo.setType(m.getType());
        vo.setName(m.getName());
        vo.setPath(m.getPath());
        vo.setComponent(m.getComponent());
        vo.setIcon(m.getIcon());
        vo.setSort(m.getSort());
        vo.setVisible(m.getVisible());
        if (withAdminFields) {
            vo.setPermission(m.getPermission());
            vo.setStatus(m.getStatus());
        }
        return vo;
    }
}
