package com.platform.cloud.bootstrap.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtProperties props;
    private final TokenBlacklist blacklist;

    @Override
    @SuppressWarnings("unchecked")
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extract(request);
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = jwtService.parseQuiet(token);
            if (claims != null && JwtService.TYPE_ACCESS.equals(claims.get(JwtService.CLAIM_TYPE))
                    && !blacklist.isRevoked(claims.getId())) {
                List<String> roles = claims.get(JwtService.CLAIM_ROLES, List.class);
                Collection<? extends GrantedAuthority> authorities = roles == null
                        ? Collections.emptyList()
                        : roles.stream().map(SimpleGrantedAuthority::new).toList();
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }

    private String extract(HttpServletRequest request) {
        String header = request.getHeader(props.getHeader());
        if (StringUtils.hasText(header) && header.startsWith(props.getTokenPrefix())) {
            return header.substring(props.getTokenPrefix().length()).trim();
        }
        return null;
    }
}
