package com.platform.cloud.mall.system.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 从当前请求里取客户端 IP、User-Agent。必须在请求线程里调用，异步任务拿不到请求上下文。
 */
public final class RequestUtils {

    /** 与 sys_login_log.ip / user_agent 列宽一致，超长截断避免插入失败 */
    private static final int IP_MAX = 64;
    private static final int UA_MAX = 255;

    private RequestUtils() {
    }

    public static HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }

    /** 部署在 Nginx 后面，真实 IP 在 X-Forwarded-For 的第一个地址或 X-Real-IP */
    public static String clientIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return "";
        }
        String ip = req.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            int comma = ip.indexOf(',');
            ip = comma > 0 ? ip.substring(0, comma) : ip;
        } else {
            ip = req.getHeader("X-Real-IP");
            if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = req.getRemoteAddr();
            }
        }
        return truncate(ip == null ? "" : ip.trim(), IP_MAX);
    }

    public static String userAgent() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return "";
        }
        String ua = req.getHeader("User-Agent");
        return truncate(ua == null ? "" : ua, UA_MAX);
    }

    public static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
