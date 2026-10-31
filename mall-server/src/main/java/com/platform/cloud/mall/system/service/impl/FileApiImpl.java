package com.platform.cloud.mall.system.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.common.config.MallProperties;
import com.platform.cloud.mall.common.util.UploadUrlValidator;
import com.platform.cloud.mall.system.api.FileApi;
import com.platform.cloud.mall.system.support.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 允许的图片前缀 = 本地上传前缀 + 当前存储实现的访问前缀。
 * <p>
 * 切换到对象存储后，切换前上传的本地图片仍然有效，所以本地前缀始终保留。
 * 当前存储前缀通过对一个探测文件名取 URL 再去掉文件名得到，与实际上传返回的地址构造方式完全一致。
 */
@Service
@RequiredArgsConstructor
public class FileApiImpl implements FileApi {

    private static final String PROBE = "probe.jpg";

    private final MallProperties mallProperties;
    private final FileStorage fileStorage;

    @Override
    public void requireUploadedImages(Collection<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        String local = mallProperties.getUpload().getUrlPrefix();
        String probeUrl = fileStorage.url(PROBE);
        String current = probeUrl.substring(0, probeUrl.length() - PROBE.length());
        for (String url : urls) {
            if (!UploadUrlValidator.isLocal(url, local) && !UploadUrlValidator.isLocal(url, current)) {
                throw new BusinessException("图片地址无效，请通过上传接口上传图片");
            }
        }
    }
}
