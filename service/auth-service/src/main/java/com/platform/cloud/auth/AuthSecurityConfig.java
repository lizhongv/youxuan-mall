package com.platform.cloud.auth;

import com.platform.cloud.bootstrap.common.security.JsonAccessDeniedHandler;
import com.platform.cloud.bootstrap.common.security.JsonAuthEntryPoint;
import com.platform.cloud.bootstrap.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * auth-service 独立运行时的安全配置。
 * <p>放在 com.platform.cloud.auth 包，避免被 bootstrap-app（扫 com.platform.cloud.bootstrap）误装载。
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class AuthSecurityConfig {

    private static final String[] WHITELIST = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/doc.html",
            "/webjars/**",
            "/favicon.ico",
            "/error"
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
                        .anyRequest().authenticated()
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    /** Phase 2 内存用户；接入用户域 DB 后会换成 DB 实现 */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder enc) {
        UserDetails admin = User.builder()
                .username("admin").password(enc.encode("admin123"))
                .roles("ADMIN", "USER").build();
        UserDetails user = User.builder()
                .username("user").password(enc.encode("user123"))
                .roles("USER").build();
        return new InMemoryUserDetailsManager(admin, user);
    }
}
