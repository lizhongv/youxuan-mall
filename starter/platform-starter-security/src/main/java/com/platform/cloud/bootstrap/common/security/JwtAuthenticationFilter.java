package com.platform.cloud.bootstrap.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 解析 Bearer access 令牌并写入 SecurityContext。
 * <p>
 * 校验顺序：签名与过期 → 类型必须是 access → 黑名单（登出）→ 版本号（改密 / 冻结后整体作废，可选）。
 * 通过后把完整声明放到请求属性 {@link #CLAIMS_ATTRIBUTE}，业务侧可读取自定义声明而不用再解析一次。
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 已认证请求的 JWT 声明（{@link Claims}）所在的请求属性名 */
    public static final String CLAIMS_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".claims";

    private final JwtService jwtService;
    private final JwtProperties props;
    private final TokenBlacklist blacklist;
    private final ObjectProvider<TokenVersionChecker> versionChecker;

    public JwtAuthenticationFilter(JwtService jwtService, JwtProperties props, TokenBlacklist blacklist,
                                   ObjectProvider<TokenVersionChecker> versionChecker) {
        this.jwtService = jwtService;
        this.props = props;
        this.blacklist = blacklist;
        this.versionChecker = versionChecker;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extract(request);
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = jwtService.parseQuiet(token);
            if (claims != null && JwtService.TYPE_ACCESS.equals(claims.get(JwtService.CLAIM_TYPE))
                    && !revoked(claims) && versionCurrent(claims)) {
                List<String> roles = claims.get(JwtService.CLAIM_ROLES, List.class);
                Collection<? extends GrantedAuthority> authorities = roles == null
                        ? Collections.emptyList()
                        : roles.stream().map(SimpleGrantedAuthority::new).toList();
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                request.setAttribute(CLAIMS_ATTRIBUTE, claims);
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 黑名单检查。Redis 异常时放行：access 令牌寿命短（默认 2 小时内），Redis 短暂故障期间让全站用户掉线的代价
     * 大于「已登出的令牌在故障期间仍可用」的风险；refresh 令牌与登录在 Redis 异常时严格失败，故障不会被放大。
     */
    private boolean revoked(Claims claims) {
        try {
            return blacklist.isRevoked(claims.getId());
        } catch (RuntimeException e) {
            log.warn("令牌黑名单检查失败，按可用性优先放行 access 令牌: {}", e.getMessage());
            return false;
        }
    }

    /** 版本号检查，异常策略同黑名单 */
    private boolean versionCurrent(Claims claims) {
        TokenVersionChecker checker = versionChecker.getIfAvailable();
        if (checker == null) {
            return true;
        }
        try {
            return checker.isCurrent(claims);
        } catch (RuntimeException e) {
            log.warn("令牌版本号检查失败，按可用性优先放行 access 令牌: {}", e.getMessage());
            return true;
        }
    }

    private String extract(HttpServletRequest request) {
        String header = request.getHeader(props.getHeader());
        if (StringUtils.hasText(header) && header.startsWith(props.getTokenPrefix())) {
            return header.substring(props.getTokenPrefix().length()).trim();
        }
        return null;
    }
}
