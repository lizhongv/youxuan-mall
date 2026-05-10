package com.platform.cloud.goods;

import com.platform.cloud.bootstrap.common.security.JsonAccessDeniedHandler;
import com.platform.cloud.bootstrap.common.security.JsonAuthEntryPoint;
import com.platform.cloud.bootstrap.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * goods-service 独立运行时的安全配置。
 * 不签发 JWT，只校验 JWT；故无需 AuthenticationManager / UserDetailsService。
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class GoodsSecurityConfig {

    private static final String[] WHITELIST = {
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/doc.html",
            "/webjars/**",
            "/favicon.ico",
            "/error"
    };

    /** 所有 GET /api/goods/** 公开（用户浏览不必登录），其他方法走 @PreAuthorize */
    private static final String[] PUBLIC_GET = {
            "/api/goods/**"
    };

    private final JwtAuthenticationFilter jwtFilter;
    private final JsonAuthEntryPoint authEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(lo -> lo.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITELIST).permitAll()
                        // _internal/** 必须登录（演示 Feign 转发 token），需在 PUBLIC_GET 通配前
                        .requestMatchers("/api/goods/_internal/**").authenticated()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
