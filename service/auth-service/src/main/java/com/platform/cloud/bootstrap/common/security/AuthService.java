package com.platform.cloud.bootstrap.common.security;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.bootstrap.web.auth.dto.LoginRequest;
import com.platform.cloud.bootstrap.web.auth.dto.LoginResponse;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final JwtProperties props;
    private final TokenBlacklist blacklist;

    public LoginResponse login(LoginRequest req) {
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        UserDetails ud = (UserDetails) authentication.getPrincipal();
        List<String> roles = ud.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return buildResponse(ud.getUsername(), roles);
    }

    public LoginResponse refresh(String refreshToken) {
        Claims claims = jwtService.parseQuiet(refreshToken);
        if (claims == null || !JwtService.TYPE_REFRESH.equals(claims.get(JwtService.CLAIM_TYPE))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "刷新令牌无效");
        }
        if (blacklist.isRevoked(claims.getId())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "刷新令牌已撤销");
        }
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.getOrDefault(JwtService.CLAIM_ROLES, List.of());
        return buildResponse(claims.getSubject(), roles);
    }

    public void logout(String accessToken) {
        Claims claims = jwtService.parseQuiet(accessToken);
        if (claims == null) {
            return;
        }
        Duration remaining = Duration.between(Instant.now(), claims.getExpiration().toInstant());
        if (!remaining.isNegative() && !remaining.isZero()) {
            blacklist.revoke(claims.getId(), remaining);
        }
    }

    private LoginResponse buildResponse(String subject, List<String> roles) {
        Map<String, Object> claims = Map.of(JwtService.CLAIM_ROLES, roles);
        String access = jwtService.generate(subject, JwtService.TYPE_ACCESS, claims, props.getAccessTtl());
        String refresh = jwtService.generate(subject, JwtService.TYPE_REFRESH, claims, props.getRefreshTtl());
        return LoginResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .tokenType("Bearer")
                .expiresIn(props.getAccessTtl().toSeconds())
                .build();
    }
}
