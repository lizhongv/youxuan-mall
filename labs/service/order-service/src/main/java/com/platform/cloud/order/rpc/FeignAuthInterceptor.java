package com.platform.cloud.order.rpc;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 透传 Authorization + X-Trace-Id 给下游 service */
@Configuration
public class FeignAuthInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;
        HttpServletRequest req = attrs.getRequest();
        String auth = req.getHeader("Authorization");
        if (StringUtils.hasText(auth)) template.header("Authorization", auth);
        String tid = req.getHeader("X-Trace-Id");
        if (StringUtils.hasText(tid)) template.header("X-Trace-Id", tid);
    }
}
