package com.platform.cloud.mall.common.util;

import com.platform.cloud.bootstrap.common.exception.BusinessException;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * 校验图片地址必须是本站上传接口返回的路径（评价图、售后凭证等会员提交的图片）。
 * <p>
 * 不校验时会员可以提交任意外链：钓鱼链接、追踪像素（泄露浏览后台的管理员 IP）、超大图拖垮页面，
 * 或 javascript: 之类的伪协议。上传接口返回的路径形如 /uploads/2026/10/31/{uuid}.jpg，这里按同样的形状校验。
 * <p>
 * 调用方式：{@code UploadUrlValidator.requireLocal(req.getImages(), mallProperties.getUpload().getUrlPrefix())}
 */
public final class UploadUrlValidator {

    /** 默认上传前缀，与 mall.upload.url-prefix 默认值一致 */
    public static final String DEFAULT_PREFIX = "/uploads/";

    /** 前缀之后只允许字母数字、斜杠、下划线、连字符与单个点（扩展名），不允许 .. 与 // */
    private static final Pattern SAFE_PATH = Pattern.compile("^[A-Za-z0-9_\\-]+(/[A-Za-z0-9_\\-]+)*\\.[A-Za-z0-9]{1,8}$");

    private static final int MAX_LENGTH = 255;

    private UploadUrlValidator() {
    }

    /** 是否为本站上传路径 */
    public static boolean isLocal(String url, String urlPrefix) {
        if (url == null || url.isEmpty() || url.length() > MAX_LENGTH) {
            return false;
        }
        String prefix = normalizePrefix(urlPrefix);
        if (!url.startsWith(prefix)) {
            return false;
        }
        String rest = url.substring(prefix.length());
        return !rest.contains("..") && SAFE_PATH.matcher(rest).matches();
    }

    /** 全部为本站上传路径，否则抛业务异常；null / 空集合视为没有图片，直接通过 */
    public static void requireLocal(Collection<String> urls, String urlPrefix) {
        if (urls == null) {
            return;
        }
        for (String url : urls) {
            if (!isLocal(url, urlPrefix)) {
                throw new BusinessException("图片地址无效，请通过上传接口上传图片");
            }
        }
    }

    private static String normalizePrefix(String urlPrefix) {
        String p = urlPrefix == null || urlPrefix.isBlank() ? DEFAULT_PREFIX : urlPrefix.trim();
        return p.endsWith("/") ? p : p + "/";
    }
}
