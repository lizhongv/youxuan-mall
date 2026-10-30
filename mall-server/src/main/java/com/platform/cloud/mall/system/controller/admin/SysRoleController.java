package com.platform.cloud.mall.system.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.system.dto.RoleMenuRequest;
import com.platform.cloud.mall.system.dto.RoleQuery;
import com.platform.cloud.mall.system.dto.RoleSaveRequest;
import com.platform.cloud.mall.system.service.SysRoleService;
import com.platform.cloud.mall.system.vo.RoleVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色与授权。
 */
@Tag(name = "角色权限")
@RestController
@RequestMapping("/admin/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @Operation(summary = "角色分页")
    @PreAuthorize("@ss.has('system:role:view')")
    @GetMapping("/page")
    public R<PageVO<RoleVO>> page(@Valid RoleQuery query) {
        return R.ok(roleService.page(query));
    }

    /** 管理员表单要选角色，所以有管理员查看权限也可以调用 */
    @Operation(summary = "全部启用角色")
    @PreAuthorize("@ss.has('system:role:view') or @ss.has('system:admin:view')")
    @GetMapping("/list")
    public R<List<RoleVO>> list() {
        return R.ok(roleService.listEnabled());
    }

    @Operation(summary = "角色详情")
    @PreAuthorize("@ss.has('system:role:view')")
    @GetMapping("/{id}")
    public R<RoleVO> detail(@PathVariable Long id) {
        return R.ok(roleService.detail(id));
    }

    @Operation(summary = "新增角色")
    @PreAuthorize("@ss.has('system:role:edit')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody RoleSaveRequest req) {
        return R.ok(roleService.create(req));
    }

    @Operation(summary = "修改角色")
    @PreAuthorize("@ss.has('system:role:edit')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RoleSaveRequest req) {
        roleService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @PreAuthorize("@ss.has('system:role:edit')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @Operation(summary = "角色已授权菜单")
    @PreAuthorize("@ss.has('system:role:view')")
    @GetMapping("/{id}/menus")
    public R<List<Long>> menus(@PathVariable Long id) {
        return R.ok(roleService.menuIds(id));
    }

    @Operation(summary = "分配菜单权限")
    @PreAuthorize("@ss.has('system:role:edit')")
    @PutMapping("/{id}/menus")
    public R<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody RoleMenuRequest req) {
        roleService.assignMenus(id, req.getMenuIds());
        return R.ok();
    }
}
