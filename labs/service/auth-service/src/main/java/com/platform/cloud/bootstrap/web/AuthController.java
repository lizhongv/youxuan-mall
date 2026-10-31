package com.platform.cloud.bootstrap.web;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.security.AuthService;
import com.platform.cloud.bootstrap.common.security.JwtProperties;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.bootstrap.web.auth.dto.LoginRequest;
import com.platform.cloud.bootstrap.web.auth.dto.LoginResponse;
import com.platform.cloud.bootstrap.web.auth.dto.RefreshRequest;
import com.platform.cloud.bootstrap.web.auth.dto.UserInfoVO;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PathVariable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Auth", description = "鉴权（登录/刷新/登出/当前用户）")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProps;
    private final UserDetailsService userDetailsService;

    @Operation(summary = "登录", security = {})
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return R.ok(authService.login(req));
    }

    @Operation(summary = "刷新 access token", security = {})
    @PostMapping("/refresh")
    public R<LoginResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return R.ok(authService.refresh(req.getRefreshToken()));
    }

    @Operation(summary = "登出（撤销当前 access token）", security = {@SecurityRequirement(name = "bearerAuth")})
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String header = request.getHeader(jwtProps.getHeader());
        if (header != null && header.startsWith(jwtProps.getTokenPrefix())) {
            authService.logout(header.substring(jwtProps.getTokenPrefix().length()).trim());
        }
        return R.ok();
    }

    @Operation(summary = "当前用户", security = {@SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/me")
    public R<Map<String, Object>> me(Authentication auth) {
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return R.ok(Map.of(
                "username", auth.getName(),
                "roles", roles,
                "authenticated", auth.isAuthenticated()
        ));
    }

    @Operation(summary = "[内部 RPC] 按用户名查信息", security = {@SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/users/{username}")
    public R<UserInfoVO> userInfo(@PathVariable String username) {
        try {
            UserDetails ud = userDetailsService.loadUserByUsername(username);
            return R.ok(UserInfoVO.builder()
                    .username(ud.getUsername())
                    .roles(ud.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                    .enabled(ud.isEnabled())
                    .accountNonLocked(ud.isAccountNonLocked())
                    .build());
        } catch (UsernameNotFoundException e) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在: " + username);
        }
    }
}
