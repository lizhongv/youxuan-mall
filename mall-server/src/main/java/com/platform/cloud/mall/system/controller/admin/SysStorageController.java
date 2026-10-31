package com.platform.cloud.mall.system.controller.admin;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.system.service.StorageTestService;
import com.platform.cloud.mall.system.vo.StorageTestVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对象存储。配置项在「系统参数」中维护，这里只提供连通性测试。
 */
@Tag(name = "系统参数-对象存储")
@RestController
@RequestMapping("/admin/system/storage")
@RequiredArgsConstructor
public class SysStorageController {

    private final StorageTestService storageTestService;

    @Operation(summary = "测试连接（上传并删除一个探测文件）")
    @PreAuthorize("@ss.has('system:config:edit')")
    @PostMapping("/test")
    public R<StorageTestVO> test() {
        return R.ok(storageTestService.test());
    }
}
