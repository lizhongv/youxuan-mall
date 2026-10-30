package com.platform.cloud.mall.member.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 取客户端 IP（记录注册 IP 用）。部署在 Nginx 后，真实 IP 在 X-Forwarded-For 第一个地址或 X-Real-IP。
 */
public final class ClientIp {

    /** 与 mbr_member.register_ip 列宽一致 */
    private static final int MAX = 64;

    private ClientIp() {
    }

    public static String current() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes sra)) {
            return "";
        }
        HttpServletRequest req = sra.getRequest();
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
        ip = ip == null ? "" : ip.trim();
        return ip.length() <= MAX ? ip : ip.substring(0, MAX);
    }
}
