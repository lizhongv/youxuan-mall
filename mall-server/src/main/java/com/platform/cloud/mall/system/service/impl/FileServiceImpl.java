package com.platform.cloud.mall.system.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.mall.common.config.MallProperties;
import com.platform.cloud.mall.common.security.RedisCounter;
import com.platform.cloud.mall.system.entity.SysFile;
import com.platform.cloud.mall.system.mapper.SysFileMapper;
import com.platform.cloud.mall.system.service.FileService;
import com.platform.cloud.mall.system.support.FileStorage;
import com.platform.cloud.mall.common.util.RequestUtils;
import com.platform.cloud.mall.system.support.UploadRules;
import com.platform.cloud.mall.system.vo.FileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 文件上传：先校验（扩展名、文件头、大小、会员配额），再落盘，最后记录 sys_file。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final int CONTENT_TYPE_MAX = 100;
    private static final String QUOTA_PREFIX = "mall:upload:member:";
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final FileStorage storage;
    private final SysFileMapper fileMapper;
    private final MallProperties props;
    private final RedisCounter counter;

    @Override
    public FileVO upload(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        MallProperties.Upload cfg = props.getUpload();
        UploadRules.checkSize(file.getSize(), cfg.getMaxSize());
        String ext = UploadRules.checkExtension(file.getOriginalFilename(), cfg.getAllowedExt());
        UploadRules.checkContent(readHeader(file), ext);
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

    @Override
    public FileVO uploadByMember(Long memberId, MultipartFile file, String bizType) {
        checkMemberQuota(memberId);
        return upload(file, bizType);
    }

    /**
     * 会员上传配额。Redis 不可用时放行：上传仍受登录、大小与类型限制，短时失去配额控制的风险
     * 小于让所有会员都无法上传头像、评价图。
     */
    void checkMemberQuota(Long memberId) {
        MallProperties.Upload cfg = props.getUpload();
        long perMinute;
        long today;
        try {
            perMinute = counter.incrementInWindow(QUOTA_PREFIX + "min:" + memberId, Duration.ofMinutes(1));
            today = counter.incrementInWindow(QUOTA_PREFIX + "day:" + LocalDate.now().format(DAY) + ":" + memberId,
                    Duration.ofDays(1));
        } catch (RuntimeException e) {
            log.warn("会员上传配额检查失败，放行 memberId={}: {}", memberId, e.getMessage());
            return;
        }
        if (perMinute > cfg.getMemberPerMinute()) {
            throw new BusinessException(ResultCode.DUPLICATE_REQUEST, "上传过于频繁，请稍后再试");
        }
        if (today > cfg.getMemberDailyLimit()) {
            throw new BusinessException("今日上传次数已达上限，请明天再试");
        }
    }

    private static byte[] readHeader(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(UploadRules.MAGIC_HEADER_LENGTH);
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败，请重试");
        }
    }
}
