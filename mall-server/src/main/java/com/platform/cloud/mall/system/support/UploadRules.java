package com.platform.cloud.mall.system.support;

import com.platform.cloud.bootstrap.common.exception.BusinessException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 上传校验与存储路径生成。纯函数，便于单测。
 * <p>
 * 只信任扩展名白名单，不信任客户端 Content-Type；存储文件名由 UUID 生成，用户只能决定扩展名（且受白名单约束），
 * 因此存储路径完全不可由用户控制。
 */
public final class UploadRules {

    private static final DateTimeFormatter DIR_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 业务类型只是归类标签，限制字符集防止被拼进路径或日志时产生歧义 */
    private static final Pattern BIZ_TYPE = Pattern.compile("^[a-z][a-z0-9_]{0,31}$");

    /** 原始文件名入库前截断到列宽 */
    private static final int ORIGINAL_NAME_MAX = 255;

    private UploadRules() {
    }

    /** 取小写扩展名；没有扩展名返回空串 */
    public static String extension(String originalName) {
        String name = baseName(originalName);
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** 校验扩展名在白名单内，返回小写扩展名 */
    public static String checkExtension(String originalName, String allowedCsv) {
        String ext = extension(originalName);
        Set<String> allowed = Arrays.stream(allowedCsv.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (ext.isEmpty() || !allowed.contains(ext)) {
            throw new BusinessException("不支持的文件类型，仅允许：" + String.join("、", allowed.stream().sorted().toList()));
        }
        return ext;
    }

    public static void checkSize(long size, long maxSize) {
        if (size <= 0) {
            throw new BusinessException("文件为空");
        }
        if (size > maxSize) {
            throw new BusinessException("文件不能超过 " + humanSize(maxSize));
        }
    }

    /** 不合法或为空时归为 common，不因为标签写错而让上传失败 */
    public static String normalizeBizType(String bizType) {
        if (bizType == null || !BIZ_TYPE.matcher(bizType).matches()) {
            return "common";
        }
        return bizType;
    }

    /** 生成 yyyy/MM/dd/uuid.ext */
    public static String buildPath(LocalDate date, String ext) {
        return date.format(DIR_FMT) + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
    }

    /** 去掉客户端可能带上的目录部分（IE 会传完整路径），只保留文件名用于展示 */
    public static String baseName(String originalName) {
        if (originalName == null) {
            return "";
        }
        String name = originalName;
        int idx = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (idx >= 0) {
            name = name.substring(idx + 1);
        }
        return name.length() > ORIGINAL_NAME_MAX ? name.substring(name.length() - ORIGINAL_NAME_MAX) : name;
    }

    private static String humanSize(long bytes) {
        if (bytes >= 1024 * 1024 && bytes % (1024 * 1024) == 0) {
            return bytes / (1024 * 1024) + "MB";
        }
        if (bytes >= 1024) {
            return bytes / 1024 + "KB";
        }
        return bytes + "B";
    }
}
