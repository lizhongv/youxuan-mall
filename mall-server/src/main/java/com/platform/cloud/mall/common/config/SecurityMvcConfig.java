package com.platform.cloud.mall.common.config;

import com.platform.cloud.mall.common.security.PasswordChangeRequiredInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册安全相关的 MVC 拦截器。放在拦截器而不是 Security 过滤器里：抛出的 BusinessException
 * 能走全局异常处理，返回与其他业务错误一致的 {code: 40301, msg} 响应体。
 */
@Configuration
@RequiredArgsConstructor
public class SecurityMvcConfig implements WebMvcConfigurer {

    private final PasswordChangeRequiredInterceptor passwordChangeRequiredInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(passwordChangeRequiredInterceptor)
                .addPathPatterns(PasswordChangeRequiredInterceptor.PATH_PATTERN)
                .excludePathPatterns(PasswordChangeRequiredInterceptor.ALLOWED_PATHS);
    }
}
