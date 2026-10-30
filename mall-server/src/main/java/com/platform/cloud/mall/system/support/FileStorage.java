package com.platform.cloud.mall.system.support;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件存储抽象。当前只有本地磁盘实现（Nginx 直接托管目录），日后换 OSS / MinIO 只需新增实现。
 */
public interface FileStorage {

    /**
     * 保存文件。
     *
     * @param relativePath 由服务端生成的相对路径（如 2026/10/31/uuid.jpg），实现仍需防御目录穿越
     */
    void store(String relativePath, InputStream in) throws IOException;

    /** 对外访问地址 */
    String url(String relativePath);
}
