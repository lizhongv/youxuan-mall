package com.platform.cloud.goods.rpc;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 把入站请求的 Authorization + X-Trace-Id 复制到出站 Feign 请求。
 * 两个目的：
 * <ol>
 *     <li>下游服务用同一 token 鉴权，无需重新签发</li>
 *     <li>traceId 链路串起来，全链路日志可关联</li>
 * </ol>
 */
@Configuration
public class FeignAuthInterceptor implements RequestInterceptor {

    private static final String AUTH = "Authorization";
    private static final String TRACE_ID = "X-Trace-Id";

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;
        HttpServletRequest req = attrs.getRequest();
        String auth = req.getHeader(AUTH);
        if (StringUtils.hasText(auth)) {
            template.header(AUTH, auth);
        }
        String tid = req.getHeader(TRACE_ID);
        if (StringUtils.hasText(tid)) {
            template.header(TRACE_ID, tid);
        }
    }
}
