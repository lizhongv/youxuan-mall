package com.platform.cloud.mall.system.controller.admin;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.system.service.FileService;
import com.platform.cloud.mall.system.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传。商品图、轮播图等各模块都要用，只要求登录，不单独设权限码。
 */
@Tag(name = "文件上传")
@RestController
@RequestMapping("/admin/system/file")
@RequiredArgsConstructor
public class SysFileController {

    private final FileService fileService;

    @Operation(summary = "上传文件")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<FileVO> upload(@RequestPart("file") MultipartFile file,
                            @RequestParam(value = "bizType", required = false) String bizType) {
        return R.ok(fileService.upload(file, bizType));
    }
}
