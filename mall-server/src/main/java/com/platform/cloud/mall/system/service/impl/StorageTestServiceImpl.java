package com.platform.cloud.mall.system.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.system.service.StorageTestService;
import com.platform.cloud.mall.system.support.storage.RoutingFileStorage;
import com.platform.cloud.mall.system.support.storage.S3FileStorage;
import com.platform.cloud.mall.system.support.storage.StorageProvider;
import com.platform.cloud.mall.system.vo.StorageTestVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 测试连接：真实走一遍「上传 → 删除」，比只调 HeadBucket 更能暴露权限问题（很多子账号只授权了读写对象、没有桶级权限）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageTestServiceImpl implements StorageTestService {

    /** 探测文件放在独立目录，万一删除失败也容易识别清理 */
    static final String PROBE_DIR = "_probe";

    private final RoutingFileStorage storage;

    @Override
    public StorageTestVO test() {
        if (storage.currentProvider() == StorageProvider.LOCAL) {
            return new StorageTestVO(StorageProvider.LOCAL.name(), null, 0, "当前为本地存储，无需测试");
        }
        S3FileStorage s3 = storage.requireS3();
        String path = PROBE_DIR + "/" + UUID.randomUUID().toString().replace("-", "") + ".txt";
        long start = System.currentTimeMillis();
        try {
            s3.store(path, new ByteArrayInputStream("mall storage probe".getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new BusinessException("上传失败：" + e.getMessage() + "，请检查 endpoint、桶名、密钥与写权限");
        }
        try {
            s3.delete(path);
        } catch (S3Exception e) {
            String code = e.awsErrorDetails() == null ? String.valueOf(e.statusCode()) : e.awsErrorDetails().errorCode();
            throw new BusinessException("上传成功但删除失败：" + code + "，请为该密钥授予删除对象权限");
        } catch (SdkException e) {
            throw new BusinessException("上传成功但删除失败：" + e.getMessage());
        }
        long elapsed = System.currentTimeMillis() - start;
        return new StorageTestVO(StorageProvider.S3.name(), s3.url(path), elapsed, "连接正常");
    }
}
