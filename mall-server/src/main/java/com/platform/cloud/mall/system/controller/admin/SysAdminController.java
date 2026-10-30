package com.platform.cloud.mall.system.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.system.dto.AdminQuery;
import com.platform.cloud.mall.system.dto.AdminSaveRequest;
import com.platform.cloud.mall.system.dto.ResetPasswordRequest;
import com.platform.cloud.mall.system.service.SysAdminService;
import com.platform.cloud.mall.system.vo.AdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员管理。
 */
@Tag(name = "管理员")
@RestController
@RequestMapping("/admin/system/admin")
@RequiredArgsConstructor
public class SysAdminController {

    private final SysAdminService adminService;

    @Operation(summary = "管理员分页")
    @PreAuthorize("@ss.has('system:admin:view')")
    @GetMapping("/page")
    public R<PageVO<AdminVO>> page(@Valid AdminQuery query) {
        return R.ok(adminService.page(query));
    }

    @Operation(summary = "管理员详情")
    @PreAuthorize("@ss.has('system:admin:view')")
    @GetMapping("/{id}")
    public R<AdminVO> detail(@PathVariable Long id) {
        return R.ok(adminService.detail(id));
    }

    @Operation(summary = "新增管理员")
    @PreAuthorize("@ss.has('system:admin:edit')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody AdminSaveRequest req) {
        return R.ok(adminService.create(req));
    }

    @Operation(summary = "修改管理员")
    @PreAuthorize("@ss.has('system:admin:edit')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AdminSaveRequest req) {
        adminService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除管理员")
    @PreAuthorize("@ss.has('system:admin:edit')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        adminService.delete(id);
        return R.ok();
    }

    @Operation(summary = "启用 / 停用管理员")
    @PreAuthorize("@ss.has('system:admin:edit')")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "重置管理员密码")
    @PreAuthorize("@ss.has('system:admin:edit')")
    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest req) {
        adminService.resetPassword(id, req.getPassword());
        return R.ok();
    }
}
