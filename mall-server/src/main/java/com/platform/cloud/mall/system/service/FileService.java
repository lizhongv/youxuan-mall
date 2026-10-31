package com.platform.cloud.mall.system.service;

import com.platform.cloud.mall.system.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传。管理端与商城端共用，商城端由会员领域调用。
 */
public interface FileService {

    /**
     * 校验扩展名白名单、文件头与大小后保存，并写入 sys_file。
     *
     * @param bizType 归类标签，如 goods / avatar / review；不合法时归为 common
     */
    FileVO upload(MultipartFile file, String bizType);

    /**
     * 会员上传：在 {@link #upload} 之前按会员限频（每分钟 / 每天次数见 mall.upload.member-*），
     * 防止注册小号把服务器磁盘刷满。
     */
    FileVO uploadByMember(Long memberId, MultipartFile file, String bizType);
}
