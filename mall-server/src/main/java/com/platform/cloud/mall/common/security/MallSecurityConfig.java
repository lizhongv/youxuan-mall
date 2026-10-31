package com.platform.cloud.mall.common.security;

import com.platform.cloud.bootstrap.common.security.JsonAccessDeniedHandler;
import com.platform.cloud.bootstrap.common.security.JsonAuthEntryPoint;
import com.platform.cloud.bootstrap.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * 安全配置：无状态 JWT。
 * <p>
 * 管理端与商城端用角色隔离：管理员令牌带 ROLE_ADMIN，会员令牌带 ROLE_MEMBER，互相不能访问对方的接口。
 * 管理端细粒度权限用 {@code @PreAuthorize("@ss.has('goods:edit')")}，见 {@link PermissionService}。
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class MallSecurityConfig {

    /**
     * 无需登录即可访问的路径。接口文档路径在 prod 模式下由 springdoc / knife4j 自身关闭，这里放行不影响。
     */
    private static final String[] PUBLIC_PATHS = {
            "/admin/auth/login", "/admin/auth/captcha", "/admin/auth/refresh",
            "/app/auth/**", "/app/public/**",
            "/doc.html", "/v3/api-docs/**", "/webjars/**", "/swagger-ui/**", "/swagger-resources/**",
            "/actuator/health", "/actuator/info", "/uploads/**", "/error"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonAuthEntryPoint authEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain mallSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 接口响应保留 Spring Security 默认安全头（nosniff、禁止缓存等）。后台页面由 Nginx 托管、
                // 不经过这里，不受影响；X-Frame-Options 放宽为 SAMEORIGIN，只为 demo 模式的 /doc.html 能同源嵌套。
                .headers(h -> h
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .exceptionHandling(e -> e.authenticationEntryPoint(authEntryPoint).accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers("/admin/**").hasRole(UserType.ADMIN.name())
                        .requestMatchers("/app/**").hasRole(UserType.MEMBER.name())
                        .requestMatchers("/actuator/**").hasRole(UserType.ADMIN.name())
                        .anyRequest().denyAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
