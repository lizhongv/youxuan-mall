package com.platform.cloud.mall.system.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.common.config.MallProperties;
import com.platform.cloud.mall.system.entity.SysFile;
import com.platform.cloud.mall.system.mapper.SysFileMapper;
import com.platform.cloud.mall.system.service.FileService;
import com.platform.cloud.mall.system.support.FileStorage;
import com.platform.cloud.mall.system.support.RequestUtils;
import com.platform.cloud.mall.system.support.UploadRules;
import com.platform.cloud.mall.system.vo.FileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

/**
 * 文件上传：先校验，再落盘，最后记录 sys_file。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final int CONTENT_TYPE_MAX = 100;

    private final FileStorage storage;
    private final SysFileMapper fileMapper;
    private final MallProperties props;

    @Override
    public FileVO upload(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        MallProperties.Upload cfg = props.getUpload();
        UploadRules.checkSize(file.getSize(), cfg.getMaxSize());
        String ext = UploadRules.checkExtension(file.getOriginalFilename(), cfg.getAllowedExt());
        String path = UploadRules.buildPath(LocalDate.now(), ext);

        try (InputStream in = file.getInputStream()) {
            storage.store(path, in);
        } catch (IOException e) {
            log.error("保存上传文件失败 path={}", path, e);
            throw new BusinessException("文件保存失败，请稍后重试");
        }

        SysFile f = new SysFile();
        f.setOriginalName(UploadRules.baseName(file.getOriginalFilename()));
        f.setPath(path);
        f.setUrl(storage.url(path));
        f.setSize(file.getSize());
        f.setContentType(RequestUtils.truncate(file.getContentType(), CONTENT_TYPE_MAX));
        f.setBizType(UploadRules.normalizeBizType(bizType));
        fileMapper.insert(f);
        return new FileVO(f.getId(), f.getUrl(), f.getOriginalName(), f.getSize().intValue());
    }
}
