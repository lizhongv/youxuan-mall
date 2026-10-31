package com.platform.cloud.mall.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 从当前请求里取客户端 IP、User-Agent。必须在请求线程里调用，异步任务拿不到请求上下文。
 * <p>
 * 全站唯一的取 IP 实现：登录日志、注册 IP、登录限流、短信频控都用它，规则不一致会让限流被绕过。
 */
public final class RequestUtils {

    /** 由 Nginx 设置为 $remote_addr（见 deploy/mall/nginx/mall.conf），客户端无法伪造 */
    public static final String REAL_IP_HEADER = "X-Real-IP";

    /** 与 sys_login_log.ip / mbr_member.register_ip 列宽一致，超长截断避免插入失败 */
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

    /** 当前请求的客户端 IP；不在请求线程中返回空串 */
    public static String clientIp() {
        HttpServletRequest req = currentRequest();
        return req == null ? "" : clientIp(req);
    }

    /**
     * 客户端 IP。
     * <ul>
     *   <li>不读 X-Forwarded-For：它的第一个值由客户端随意填写，用它做限流等于让攻击者自选 IP。</li>
     *   <li>只有直连方是本机 / 内网地址（即请求来自我们自己的 Nginx）时才信任 X-Real-IP；
     *       公网直连应用端口时该头可伪造，直接用 remoteAddr。</li>
     * </ul>
     */
    public static String clientIp(HttpServletRequest req) {
        String remote = req.getRemoteAddr();
        String ip = remote;
        if (isTrustedProxy(remote)) {
            String real = req.getHeader(REAL_IP_HEADER);
            if (StringUtils.hasText(real) && !"unknown".equalsIgnoreCase(real.trim())) {
                ip = real.trim();
            }
        }
        return truncate(ip == null ? "" : ip.trim(), IP_MAX);
    }

    /** 回环、内网（10/8、172.16/12、192.168/16、fc00::/7）视为反向代理；Docker 网桥地址也在其中 */
    static boolean isTrustedProxy(String addr) {
        if (!StringUtils.hasText(addr) || !looksLikeIpLiteral(addr)) {
            return false;
        }
        try {
            // 参数已确认是 IP 字面量，getByName 不会触发 DNS 查询
            InetAddress a = InetAddress.getByName(addr);
            if (a.isLoopbackAddress() || a.isSiteLocalAddress()) {
                return true;
            }
            byte[] b = a.getAddress();
            return b.length == 16 && (b[0] & 0xFE) == 0xFC;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private static boolean looksLikeIpLiteral(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
                    || c == '.' || c == ':';
            if (!ok) {
                return false;
            }
        }
        return true;
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
