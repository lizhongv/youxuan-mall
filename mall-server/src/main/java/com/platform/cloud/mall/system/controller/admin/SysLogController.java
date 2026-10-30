package com.platform.cloud.mall.system.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.system.dto.LoginLogQuery;
import com.platform.cloud.mall.system.dto.OperLogQuery;
import com.platform.cloud.mall.system.service.SysLoginLogService;
import com.platform.cloud.mall.system.service.SysOperLogService;
import com.platform.cloud.mall.system.vo.LoginLogVO;
import com.platform.cloud.mall.system.vo.OperLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志与登录日志（只读）。
 */
@Tag(name = "系统日志")
@RestController
@RequestMapping("/admin/system")
@RequiredArgsConstructor
public class SysLogController {

    private final SysOperLogService operLogService;
    private final SysLoginLogService loginLogService;

    @Operation(summary = "操作日志分页")
    @PreAuthorize("@ss.has('system:log:view')")
    @GetMapping("/oper-log/page")
    public R<PageVO<OperLogVO>> operLogs(@Valid OperLogQuery query) {
        return R.ok(operLogService.page(query));
    }

    @Operation(summary = "登录日志分页")
    @PreAuthorize("@ss.has('system:log:view')")
    @GetMapping("/login-log/page")
    public R<PageVO<LoginLogVO>> loginLogs(@Valid LoginLogQuery query) {
        return R.ok(loginLogService.page(query));
    }
}
