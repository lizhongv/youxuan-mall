package com.platform.cloud.mall.system.controller.admin;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.system.dto.ConfigBatchRequest;
import com.platform.cloud.mall.system.service.SysConfigService;
import com.platform.cloud.mall.system.vo.ConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统参数。
 */
@Tag(name = "系统参数")
@RestController
@RequestMapping("/admin/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService configService;

    @Operation(summary = "全部参数")
    @PreAuthorize("@ss.has('system:config:view')")
    @GetMapping("/list")
    public R<List<ConfigVO>> list() {
        return R.ok(configService.list());
    }

    @Operation(summary = "批量修改参数")
    @PreAuthorize("@ss.has('system:config:edit')")
    @PutMapping("/batch")
    public R<Void> batch(@Valid @RequestBody ConfigBatchRequest req) {
        configService.batchUpdate(req.getItems());
        return R.ok();
    }
}
