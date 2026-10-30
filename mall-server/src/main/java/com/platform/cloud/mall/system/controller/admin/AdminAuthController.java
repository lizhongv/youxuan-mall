package com.platform.cloud.mall.system.controller.admin;

import com.platform.cloud.bootstrap.common.security.JwtProperties;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.system.dto.AdminLoginRequest;
import com.platform.cloud.mall.system.dto.AdminProfileRequest;
import com.platform.cloud.mall.system.dto.ChangePasswordRequest;
import com.platform.cloud.mall.system.dto.RefreshTokenRequest;
import com.platform.cloud.mall.system.service.AdminAuthService;
import com.platform.cloud.mall.system.vo.AdminInfoVO;
import com.platform.cloud.mall.system.vo.TokenVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端认证与个人中心。login / refresh 在安全配置里公开，其余需要管理员令牌。
 */
@Tag(name = "认证与个人")
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService authService;
    private final JwtProperties jwtProperties;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<TokenVO> login(@Valid @RequestBody AdminLoginRequest req) {
        return R.ok(authService.login(req));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public R<TokenVO> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return R.ok(authService.refresh(req.getRefreshToken()));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String header = request.getHeader(jwtProperties.getHeader());
        String prefix = jwtProperties.getTokenPrefix();
        if (header != null && header.startsWith(prefix)) {
            authService.logout(header.substring(prefix.length()).trim());
        }
        return R.ok();
    }

    @Operation(summary = "当前管理员信息、权限与菜单")
    @GetMapping("/me")
    public R<AdminInfoVO> me() {
        return R.ok(authService.me(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public R<Void> updateProfile(@Valid @RequestBody AdminProfileRequest req) {
        authService.updateProfile(SecurityUtils.currentUserId(), req);
        return R.ok();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(SecurityUtils.currentUserId(), req);
        return R.ok();
    }
}
