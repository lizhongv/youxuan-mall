package com.platform.cloud.mall.system.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.system.dto.RoleQuery;
import com.platform.cloud.mall.system.dto.RoleSaveRequest;
import com.platform.cloud.mall.system.entity.SysRole;
import com.platform.cloud.mall.system.vo.RoleVO;

import java.util.List;

/**
 * 角色与角色授权。
 */
public interface SysRoleService {

    PageVO<RoleVO> page(RoleQuery query);

    RoleVO detail(Long id);

    Long create(RoleSaveRequest req);

    void update(Long id, RoleSaveRequest req);

    void delete(Long id);

    /** 全部启用的角色，供管理员表单下拉 */
    List<RoleVO> listEnabled();

    /** 角色已授权的菜单 ID */
    List<Long> menuIds(Long roleId);

    /** 整体覆盖角色授权 */
    void assignMenus(Long roleId, List<Long> menuIds);

    /** 管理员当前拥有的启用角色，签发令牌时使用 */
    List<SysRole> enabledRolesOfAdmin(Long adminId);
}
