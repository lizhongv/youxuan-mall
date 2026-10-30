package com.platform.cloud.mall.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.security.JwtProperties;
import com.platform.cloud.bootstrap.common.security.JwtService;
import com.platform.cloud.bootstrap.common.security.TokenBlacklist;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.mall.common.security.UserType;
import com.platform.cloud.mall.system.dto.AdminLoginRequest;
import com.platform.cloud.mall.system.dto.AdminProfileRequest;
import com.platform.cloud.mall.system.dto.ChangePasswordRequest;
import com.platform.cloud.mall.system.entity.SysAdmin;
import com.platform.cloud.mall.system.entity.SysRole;
import com.platform.cloud.mall.system.mapper.SysAdminMapper;
import com.platform.cloud.mall.system.service.AdminAuthService;
import com.platform.cloud.mall.system.service.SysLoginLogService;
import com.platform.cloud.mall.system.service.SysMenuService;
import com.platform.cloud.mall.system.service.SysRoleService;
import com.platform.cloud.mall.system.support.LoginFailureGuard;
import com.platform.cloud.mall.system.support.RequestUtils;
import com.platform.cloud.mall.system.support.SystemConstants;
import com.platform.cloud.mall.system.vo.AdminInfoVO;
import com.platform.cloud.mall.system.vo.TokenVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理员认证。
 * <p>
 * access 令牌的 roles 声明 = ROLE_ADMIN + ROLE_&lt;角色编码&gt; + 权限码，由 JwtAuthenticationFilter 直接转成 authorities，
 * 因此每次请求不用查库；代价是权限调整要等令牌刷新（refresh 时会重新读库）才生效。
 */
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final SysAdminMapper adminMapper;
    private final SysRoleService roleService;
    private final SysMenuService menuService;
    private final SysLoginLogService loginLogService;
    private final LoginFailureGuard failureGuard;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final TokenBlacklist tokenBlacklist;

    @Override
    public TokenVO login(AdminLoginRequest req) {
        String username = req.getUsername().trim();
        failureGuard.checkNotLocked(username);

        SysAdmin admin = adminMapper.selectOne(new LambdaQueryWrapper<SysAdmin>().eq(SysAdmin::getUsername, username));
        if (admin == null || !passwordEncoder.matches(req.getPassword(), admin.getPassword())) {
            String msg = failureGuard.onFailure(username);
            loginLogService.record(UserType.ADMIN, admin == null ? null : admin.getId(), username, false, msg);
            throw new BusinessException(msg);
        }
        if (!Integer.valueOf(SystemConstants.ENABLED).equals(admin.getStatus())) {
            loginLogService.record(UserType.ADMIN, admin.getId(), username, false, "账号已停用");
            throw new BusinessException("账号已停用，请联系管理员");
        }
        failureGuard.reset(username);

        adminMapper.update(null, new LambdaUpdateWrapper<SysAdmin>()
                .set(SysAdmin::getLastLoginAt, LocalDateTime.now())
                .set(SysAdmin::getLastLoginIp, RequestUtils.clientIp())
                .eq(SysAdmin::getId, admin.getId()));
        loginLogService.record(UserType.ADMIN, admin.getId(), username, true, "登录成功");
        return issueTokens(admin);
    }

    @Override
    public TokenVO refresh(String refreshToken) {
        Claims claims = jwtService.parseQuiet(refreshToken);
        if (claims == null
                || !JwtService.TYPE_REFRESH.equals(claims.get(JwtService.CLAIM_TYPE))
                || !UserType.ADMIN.name().equals(claims.get(SystemConstants.CLAIM_UTYPE))
                || tokenBlacklist.isRevoked(claims.getId())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        SysAdmin admin = adminMapper.selectById(parseId(claims.getSubject()));
        if (admin == null || !Integer.valueOf(SystemConstants.ENABLED).equals(admin.getStatus())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在或已停用");
        }
        // refresh 令牌轮换：旧的立即作废，被窃取的 refresh 令牌最多只能用一次
        revoke(claims);
        return issueTokens(admin);
    }

    @Override
    public void logout(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return;
        }
        Claims claims = jwtService.parseQuiet(accessToken);
        if (claims != null) {
            revoke(claims);
        }
    }

    @Override
    public AdminInfoVO me(Long adminId) {
        SysAdmin admin = requireAdmin(adminId);
        List<SysRole> roles = roleService.enabledRolesOfAdmin(adminId);
        boolean superAdmin = isSuperAdmin(roles);
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();

        AdminInfoVO vo = new AdminInfoVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setNickname(admin.getNickname());
        vo.setAvatar(admin.getAvatar());
        vo.setRoles(roles.stream().map(SysRole::getCode).toList());
        vo.setPermissions(menuService.permissionsOf(roleIds, superAdmin));
        vo.setMenus(menuService.menuTreeOf(roleIds, superAdmin));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long adminId, AdminProfileRequest req) {
        requireAdmin(adminId);
        adminMapper.update(null, new LambdaUpdateWrapper<SysAdmin>()
                .set(req.getNickname() != null, SysAdmin::getNickname, req.getNickname())
                .set(req.getAvatar() != null, SysAdmin::getAvatar, req.getAvatar())
                .set(req.getMobile() != null, SysAdmin::getMobile, req.getMobile())
                .set(req.getEmail() != null, SysAdmin::getEmail, req.getEmail())
                .set(SysAdmin::getUpdatedAt, LocalDateTime.now())
                .set(SysAdmin::getUpdatedBy, adminId)
                .eq(SysAdmin::getId, adminId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long adminId, ChangePasswordRequest req) {
        SysAdmin admin = requireAdmin(adminId);
        if (!passwordEncoder.matches(req.getOldPassword(), admin.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        if (req.getOldPassword().equals(req.getNewPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        adminMapper.update(null, new LambdaUpdateWrapper<SysAdmin>()
                .set(SysAdmin::getPassword, passwordEncoder.encode(req.getNewPassword()))
                .set(SysAdmin::getUpdatedAt, LocalDateTime.now())
                .set(SysAdmin::getUpdatedBy, adminId)
                .eq(SysAdmin::getId, adminId));
    }

    /** 签发令牌对，角色和权限每次都从库里重新读 */
    TokenVO issueTokens(SysAdmin admin) {
        List<SysRole> roles = roleService.enabledRolesOfAdmin(admin.getId());
        boolean superAdmin = isSuperAdmin(roles);
        List<String> permissions = menuService.permissionsOf(roles.stream().map(SysRole::getId).toList(), superAdmin);

        List<String> authorities = new ArrayList<>();
        authorities.add(UserType.ADMIN.role());
        roles.forEach(r -> authorities.add(SystemConstants.ROLE_PREFIX + r.getCode()));
        authorities.addAll(permissions);

        String subject = String.valueOf(admin.getId());
        Duration accessTtl = jwtProperties.getAccessTtl();
        String access = jwtService.generate(subject, JwtService.TYPE_ACCESS, Map.of(
                JwtService.CLAIM_ROLES, authorities,
                SystemConstants.CLAIM_UTYPE, UserType.ADMIN.name()), accessTtl);
        String refresh = jwtService.generate(subject, JwtService.TYPE_REFRESH,
                Map.of(SystemConstants.CLAIM_UTYPE, UserType.ADMIN.name()), jwtProperties.getRefreshTtl());
        return new TokenVO(access, refresh, (int) accessTtl.toSeconds());
    }

    private void revoke(Claims claims) {
        if (claims.getExpiration() == null) {
            return;
        }
        Duration remaining = Duration.between(Instant.now(), claims.getExpiration().toInstant());
        tokenBlacklist.revoke(claims.getId(), remaining);
    }

    private static boolean isSuperAdmin(List<SysRole> roles) {
        return roles.stream().anyMatch(r -> SystemConstants.SUPER_ADMIN_CODE.equals(r.getCode()));
    }

    private SysAdmin requireAdmin(Long adminId) {
        SysAdmin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在");
        }
        return admin;
    }

    private static Long parseId(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
    }
}
