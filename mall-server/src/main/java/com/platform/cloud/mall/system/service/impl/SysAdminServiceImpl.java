package com.platform.cloud.mall.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.common.util.MaskUtils;
import com.platform.cloud.mall.system.dto.AdminQuery;
import com.platform.cloud.mall.system.dto.AdminSaveRequest;
import com.platform.cloud.mall.system.entity.SysAdmin;
import com.platform.cloud.mall.system.entity.SysAdminRole;
import com.platform.cloud.mall.system.entity.SysRole;
import com.platform.cloud.mall.system.mapper.SysAdminMapper;
import com.platform.cloud.mall.system.mapper.SysAdminRoleMapper;
import com.platform.cloud.mall.system.mapper.SysRoleMapper;
import com.platform.cloud.mall.system.service.SysAdminService;
import com.platform.cloud.mall.system.support.SystemConstants;
import com.platform.cloud.mall.system.vo.AdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理员维护。
 */
@Service
@RequiredArgsConstructor
public class SysAdminServiceImpl implements SysAdminService {

    private final SysAdminMapper adminMapper;
    private final SysAdminRoleMapper adminRoleMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageVO<AdminVO> page(AdminQuery q) {
        String kw = StringUtils.hasText(q.getKeyword()) ? q.getKeyword().trim() : null;
        LambdaQueryWrapper<SysAdmin> w = new LambdaQueryWrapper<SysAdmin>()
                .and(kw != null, x -> x.like(SysAdmin::getUsername, kw).or().like(SysAdmin::getNickname, kw)
                        .or().like(SysAdmin::getMobile, kw))
                .eq(q.getStatus() != null, SysAdmin::getStatus, q.getStatus())
                .orderByDesc(SysAdmin::getCreatedAt);
        Page<SysAdmin> page = adminMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w);
        Map<Long, List<SysRole>> roles = rolesOf(page.getRecords().stream().map(SysAdmin::getId).toList());
        // 列表手机号脱敏；详情（编辑表单）返回原值
        return PageVO.of(page, a -> {
            AdminVO vo = toVO(a, roles.getOrDefault(a.getId(), List.of()));
            vo.setMobile(MaskUtils.mobile(a.getMobile()));
            return vo;
        });
    }

    @Override
    public AdminVO detail(Long id) {
        SysAdmin admin = require(id);
        return toVO(admin, rolesOf(List.of(id)).getOrDefault(id, List.of()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AdminSaveRequest req) {
        String username = req.getUsername().trim();
        if (!StringUtils.hasText(req.getPassword())) {
            throw new BusinessException("请设置登录密码");
        }
        checkUsernameUnique(username);
        List<Long> roleIds = validRoleIds(req.getRoleIds());

        SysAdmin admin = new SysAdmin();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(req.getPassword()));
        admin.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname() : username);
        admin.setAvatar(Objects.toString(req.getAvatar(), ""));
        admin.setMobile(Objects.toString(req.getMobile(), ""));
        admin.setEmail(Objects.toString(req.getEmail(), ""));
        admin.setStatus(req.getStatus() == null ? SystemConstants.ENABLED : req.getStatus());
        admin.setRemark(Objects.toString(req.getRemark(), ""));
        admin.setLastLoginIp("");
        adminMapper.insert(admin);
        saveRoles(admin.getId(), roleIds);
        return admin.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AdminSaveRequest req) {
        SysAdmin admin = require(id);
        if (req.getStatus() != null && req.getStatus() == SystemConstants.DISABLED) {
            checkProtected(admin, "停用");
        }
        // 用户名是登录凭据且出现在操作日志里，创建后不允许修改，req.username 在修改时忽略
        adminMapper.update(null, new LambdaUpdateWrapper<SysAdmin>()
                .set(req.getNickname() != null, SysAdmin::getNickname, req.getNickname())
                .set(req.getAvatar() != null, SysAdmin::getAvatar, req.getAvatar())
                .set(req.getMobile() != null, SysAdmin::getMobile, req.getMobile())
                .set(req.getEmail() != null, SysAdmin::getEmail, req.getEmail())
                .set(req.getStatus() != null, SysAdmin::getStatus, req.getStatus())
                .set(req.getRemark() != null, SysAdmin::getRemark, req.getRemark())
                .set(SysAdmin::getUpdatedAt, LocalDateTime.now())
                .set(SysAdmin::getUpdatedBy, SecurityUtils.currentUserIdOrNull())
                .eq(SysAdmin::getId, id));
        if (req.getRoleIds() != null) {
            List<Long> roleIds = validRoleIds(req.getRoleIds());
            if (SystemConstants.ADMIN_USERNAME.equals(admin.getUsername())) {
                ensureSuperAdminKept(roleIds);
            }
            adminRoleMapper.delete(new LambdaQueryWrapper<SysAdminRole>().eq(SysAdminRole::getAdminId, id));
            saveRoles(id, roleIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysAdmin admin = require(id);
        checkProtected(admin, "删除");
        // uk_username 包含 deleted 列，逻辑删除后改名释放用户名，否则同名账号删第二次会撞唯一键
        adminMapper.update(null, new LambdaUpdateWrapper<SysAdmin>()
                .set(SysAdmin::getUsername, admin.getUsername() + "#" + id)
                .eq(SysAdmin::getId, id));
        adminMapper.deleteById(id);
        adminRoleMapper.delete(new LambdaQueryWrapper<SysAdminRole>().eq(SysAdminRole::getAdminId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != SystemConstants.ENABLED && status != SystemConstants.DISABLED)) {
            throw new BusinessException("状态只能是 1 或 0");
        }
        SysAdmin admin = require(id);
        if (status == SystemConstants.DISABLED) {
            checkProtected(admin, "停用");
        }
        adminMapper.update(null, new LambdaUpdateWrapper<SysAdmin>()
                .set(SysAdmin::getStatus, status)
                .eq(SysAdmin::getId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String password) {
        require(id);
        adminMapper.update(null, new LambdaUpdateWrapper<SysAdmin>()
                .set(SysAdmin::getPassword, passwordEncoder.encode(password))
                .eq(SysAdmin::getId, id));
    }

    private void checkProtected(SysAdmin admin, String action) {
        if (SystemConstants.ADMIN_USERNAME.equals(admin.getUsername())) {
            throw new BusinessException("内置管理员 admin 不能" + action);
        }
        if (admin.getId().equals(SecurityUtils.currentUserIdOrNull())) {
            throw new BusinessException("不能" + action + "当前登录的账号");
        }
    }

    private void ensureSuperAdminKept(List<Long> roleIds) {
        SysRole superRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, SystemConstants.SUPER_ADMIN_CODE));
        if (superRole != null && !roleIds.contains(superRole.getId())) {
            throw new BusinessException("内置管理员 admin 必须保留超级管理员角色");
        }
    }

    private void checkUsernameUnique(String username) {
        Long count = adminMapper.selectCount(new LambdaQueryWrapper<SysAdmin>().eq(SysAdmin::getUsername, username));
        if (count != null && count > 0) {
            throw new BusinessException("用户名已存在");
        }
    }

    private List<Long> validRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        Set<Long> distinct = new LinkedHashSet<>(roleIds);
        distinct.remove(null);
        if (distinct.isEmpty()) {
            return List.of();
        }
        List<SysRole> roles = roleMapper.selectBatchIds(distinct);
        if (roles.size() != distinct.size()) {
            throw new BusinessException("角色不存在");
        }
        return new ArrayList<>(distinct);
    }

    private void saveRoles(Long adminId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            adminRoleMapper.insert(new SysAdminRole(adminId, roleId));
        }
    }

    private Map<Long, List<SysRole>> rolesOf(Collection<Long> adminIds) {
        if (adminIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysAdminRole> links = adminRoleMapper.selectList(
                new LambdaQueryWrapper<SysAdminRole>().in(SysAdminRole::getAdminId, adminIds));
        if (links.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, SysRole> roles = roleMapper.selectBatchIds(
                        links.stream().map(SysAdminRole::getRoleId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(SysRole::getId, Function.identity()));
        return links.stream()
                .filter(l -> roles.containsKey(l.getRoleId()))
                .collect(Collectors.groupingBy(SysAdminRole::getAdminId,
                        Collectors.mapping(l -> roles.get(l.getRoleId()), Collectors.toList())));
    }

    private SysAdmin require(Long id) {
        SysAdmin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        return admin;
    }

    private static AdminVO toVO(SysAdmin a, List<SysRole> roles) {
        AdminVO vo = new AdminVO();
        vo.setId(a.getId());
        vo.setUsername(a.getUsername());
        vo.setNickname(a.getNickname());
        vo.setAvatar(a.getAvatar());
        vo.setMobile(a.getMobile());
        vo.setEmail(a.getEmail());
        vo.setStatus(a.getStatus());
        vo.setRoleIds(roles.stream().map(SysRole::getId).toList());
        vo.setRoleNames(roles.stream().map(SysRole::getName).toList());
        vo.setLastLoginAt(a.getLastLoginAt());
        vo.setRemark(a.getRemark());
        vo.setCreatedAt(a.getCreatedAt());
        return vo;
    }
}
