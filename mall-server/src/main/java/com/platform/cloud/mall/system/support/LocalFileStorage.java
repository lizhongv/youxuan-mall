package com.platform.cloud.mall.system.support;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.common.config.MallProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地磁盘存储，根目录为 {@code mall.upload.dir}（容器内挂载数据卷）。
 */
@Component
public class LocalFileStorage implements FileStorage {

    private final Path root;
    private final String urlPrefix;

    public LocalFileStorage(MallProperties props) {
        this.root = Paths.get(props.getUpload().getDir()).toAbsolutePath().normalize();
        String prefix = props.getUpload().getUrlPrefix();
        this.urlPrefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
    }

    @Override
    public void store(String relativePath, InputStream in) throws IOException {
        Path target = resolveSafe(relativePath);
        Files.createDirectories(target.getParent());
        Files.copy(in, target);
    }

    @Override
    public String url(String relativePath) {
        return urlPrefix + "/" + relativePath;
    }

    public Path root() {
        return root;
    }

    /**
     * 解析为绝对路径并确认仍在根目录内。路径本来由服务端生成，这里是纵深防御：
     * 即使上游拼接出 {@code ../}、绝对路径或 Windows 盘符，也不会写到根目录之外。
     */
    Path resolveSafe(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new BusinessException("非法的文件路径");
        }
        Path target;
        try {
            target = root.resolve(relativePath).normalize();
        } catch (RuntimeException e) {
            throw new BusinessException("非法的文件路径");
        }
        if (!target.startsWith(root) || target.equals(root)) {
            throw new BusinessException("非法的文件路径");
        }
        return target;
    }
}
