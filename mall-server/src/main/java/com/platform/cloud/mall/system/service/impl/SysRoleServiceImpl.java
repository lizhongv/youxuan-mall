package com.platform.cloud.mall.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.system.dto.RoleQuery;
import com.platform.cloud.mall.system.dto.RoleSaveRequest;
import com.platform.cloud.mall.system.entity.SysAdminRole;
import com.platform.cloud.mall.system.entity.SysMenu;
import com.platform.cloud.mall.system.entity.SysRole;
import com.platform.cloud.mall.system.entity.SysRoleMenu;
import com.platform.cloud.mall.system.mapper.SysAdminRoleMapper;
import com.platform.cloud.mall.system.mapper.SysMenuMapper;
import com.platform.cloud.mall.system.mapper.SysRoleMapper;
import com.platform.cloud.mall.system.mapper.SysRoleMenuMapper;
import com.platform.cloud.mall.system.service.SysRoleService;
import com.platform.cloud.mall.system.support.SystemConstants;
import com.platform.cloud.mall.system.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 角色维护。超级管理员角色是权限判断的特例（直接放行），因此不能删除、停用或改编码。
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysAdminRoleMapper adminRoleMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public PageVO<RoleVO> page(RoleQuery q) {
        String kw = StringUtils.hasText(q.getKeyword()) ? q.getKeyword().trim() : null;
        LambdaQueryWrapper<SysRole> w = new LambdaQueryWrapper<SysRole>()
                .and(kw != null, x -> x.like(SysRole::getName, kw).or().like(SysRole::getCode, kw))
                .eq(q.getStatus() != null, SysRole::getStatus, q.getStatus())
                .orderByAsc(SysRole::getSort).orderByAsc(SysRole::getId);
        return PageVO.of(roleMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w), SysRoleServiceImpl::toVO);
    }

    @Override
    public RoleVO detail(Long id) {
        return toVO(require(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleSaveRequest req) {
        checkCodeUnique(req.getCode(), null);
        SysRole role = new SysRole();
        role.setName(req.getName().trim());
        role.setCode(req.getCode());
        role.setSort(req.getSort() == null ? 0 : req.getSort());
        role.setStatus(req.getStatus() == null ? SystemConstants.ENABLED : req.getStatus());
        role.setRemark(Objects.toString(req.getRemark(), ""));
        roleMapper.insert(role);
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RoleSaveRequest req) {
        SysRole role = require(id);
        if (SystemConstants.SUPER_ADMIN_CODE.equals(role.getCode())) {
            if (!SystemConstants.SUPER_ADMIN_CODE.equals(req.getCode())) {
                throw new BusinessException("超级管理员角色的编码不能修改");
            }
            if (req.getStatus() != null && req.getStatus() == SystemConstants.DISABLED) {
                throw new BusinessException("超级管理员角色不能停用");
            }
        }
        if (!role.getCode().equals(req.getCode())) {
            checkCodeUnique(req.getCode(), id);
        }
        roleMapper.update(null, new LambdaUpdateWrapper<SysRole>()
                .set(SysRole::getName, req.getName().trim())
                .set(SysRole::getCode, req.getCode())
                .set(req.getSort() != null, SysRole::getSort, req.getSort())
                .set(req.getStatus() != null, SysRole::getStatus, req.getStatus())
                .set(req.getRemark() != null, SysRole::getRemark, req.getRemark())
                .eq(SysRole::getId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = require(id);
        if (SystemConstants.SUPER_ADMIN_CODE.equals(role.getCode())) {
            throw new BusinessException("超级管理员角色不能删除");
        }
        Long bound = adminRoleMapper.selectCount(new LambdaQueryWrapper<SysAdminRole>().eq(SysAdminRole::getRoleId, id));
        if (bound != null && bound > 0) {
            throw new BusinessException("该角色已分配给 " + bound + " 个管理员，请先取消分配");
        }
        // uk_code 含 deleted 列，改名释放编码，便于以后重建同编码角色
        roleMapper.update(null, new LambdaUpdateWrapper<SysRole>()
                .set(SysRole::getCode, role.getCode() + "#" + id)
                .eq(SysRole::getId, id));
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
    }

    @Override
    public List<RoleVO> listEnabled() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getStatus, SystemConstants.ENABLED)
                        .orderByAsc(SysRole::getSort).orderByAsc(SysRole::getId))
                .stream().map(SysRoleServiceImpl::toVO).toList();
    }

    @Override
    public List<Long> menuIds(Long roleId) {
        require(roleId);
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        require(roleId);
        Set<Long> ids = new LinkedHashSet<>(menuIds);
        ids.remove(null);
        if (!ids.isEmpty()) {
            List<SysMenu> menus = menuMapper.selectBatchIds(ids);
            if (menus.size() != ids.size()) {
                throw new BusinessException("包含不存在的菜单，请刷新后重试");
            }
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        for (Long menuId : ids) {
            roleMenuMapper.insert(new SysRoleMenu(roleId, menuId));
        }
    }

    @Override
    public List<SysRole> enabledRolesOfAdmin(Long adminId) {
        List<Long> roleIds = adminRoleMapper.selectList(
                        new LambdaQueryWrapper<SysAdminRole>().eq(SysAdminRole::getAdminId, adminId))
                .stream().map(SysAdminRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, SystemConstants.ENABLED)
                .orderByAsc(SysRole::getSort));
    }

    private void checkCodeUnique(String code, Long excludeId) {
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, code)
                .ne(excludeId != null, SysRole::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("角色编码已存在");
        }
    }

    private SysRole require(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    private static RoleVO toVO(SysRole r) {
        RoleVO vo = new RoleVO();
        vo.setId(r.getId());
        vo.setName(r.getName());
        vo.setCode(r.getCode());
        vo.setSort(r.getSort());
        vo.setStatus(r.getStatus());
        vo.setRemark(r.getRemark());
        vo.setCreatedAt(r.getCreatedAt());
        return vo;
    }
}
