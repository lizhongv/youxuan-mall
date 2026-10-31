package com.platform.cloud.mall.system.support.storage;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.system.api.ConfigApi;
import com.platform.cloud.mall.system.api.InfraConfigKeys;
import com.platform.cloud.mall.system.support.FileStorage;
import com.platform.cloud.mall.system.support.LocalFileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Function;

/**
 * 按 sys_config 的 {@code storage.provider} 在本地磁盘与 S3 对象存储之间切换。
 * <p>
 * 标为 @Primary，FileServiceImpl 注入的 FileStorage 即本类，上传流程与校验逻辑不受影响。
 * 切换只影响之后的新上传：已上传文件的完整 URL 已写入 sys_file 与业务表，本地文件仍由 Nginx 托管，旧链接继续可用。
 * S3 配置变化（换桶、换密钥）时重建客户端，无需重启。
 */
@Slf4j
@Primary
@Component
public class RoutingFileStorage implements FileStorage, DisposableBean {

    private final LocalFileStorage local;
    private final ConfigApi config;
    private final Function<S3Settings, S3FileStorage> factory;

    private S3FileStorage cached;

    @Autowired
    public RoutingFileStorage(LocalFileStorage local, ConfigApi config) {
        this(local, config, S3FileStorage::create);
    }

    RoutingFileStorage(LocalFileStorage local, ConfigApi config, Function<S3Settings, S3FileStorage> factory) {
        this.local = local;
        this.config = config;
        this.factory = factory;
    }

    @Override
    public void store(String relativePath, InputStream in) throws IOException {
        current().store(relativePath, in);
    }

    @Override
    public String url(String relativePath) {
        return current().url(relativePath);
    }

    /** 当前生效的存储实现 */
    public FileStorage current() {
        S3Settings s = S3Settings.from(config);
        if (s.provider() == StorageProvider.LOCAL) {
            return local;
        }
        return s3(s);
    }

    /** 当前配置的存储方式 */
    public StorageProvider currentProvider() {
        return S3Settings.from(config).provider();
    }

    /** 当前配置下的 S3 存储；未选择 S3 时报错，供「测试连接」使用 */
    public S3FileStorage requireS3() {
        S3Settings s = S3Settings.from(config);
        if (s.provider() != StorageProvider.S3) {
            throw new BusinessException("当前存储方式不是 S3 对象存储");
        }
        return s3(s);
    }

    /** synchronized：配置切换瞬间只构建一次客户端，并关闭旧客户端释放连接 */
    synchronized S3FileStorage s3(S3Settings s) {
        if (s.provider() == null) {
            log.error("无法识别的存储方式 {}={}", InfraConfigKeys.STORAGE_PROVIDER, s.rawProvider());
            throw new BusinessException("存储方式配置错误：" + s.rawProvider() + "（可选 LOCAL / S3）");
        }
        if (cached != null && cached.settings().equals(s)) {
            return cached;
        }
        List<String> missing = s.missingFields();
        if (!missing.isEmpty()) {
            log.error("对象存储配置不完整，缺少 {}", missing);
            throw new BusinessException("对象存储配置不完整，缺少：" + String.join("、", missing));
        }
        S3FileStorage created;
        try {
            created = factory.apply(s);
        } catch (RuntimeException e) {
            // endpoint 写错（如缺少 https://）等会在构建时抛出，信息里不含密钥
            log.error("对象存储客户端初始化失败 {} msg={}", s, e.getMessage());
            throw new BusinessException("对象存储客户端初始化失败：" + e.getMessage());
        }
        S3FileStorage old = cached;
        cached = created;
        log.info("对象存储已切换 {}", s);
        closeQuietly(old);
        return created;
    }

    @Override
    public synchronized void destroy() {
        closeQuietly(cached);
        cached = null;
    }

    private static void closeQuietly(S3FileStorage storage) {
        if (storage == null) {
            return;
        }
        try {
            storage.close();
        } catch (RuntimeException e) {
            log.warn("关闭旧的对象存储客户端失败：{}", e.getMessage());
        }
    }
}
