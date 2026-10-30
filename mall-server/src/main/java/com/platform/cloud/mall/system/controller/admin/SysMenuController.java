package com.platform.cloud.mall.system.controller.admin;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.system.dto.MenuSaveRequest;
import com.platform.cloud.mall.system.service.SysMenuService;
import com.platform.cloud.mall.system.vo.MenuTreeVO;
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
 * 菜单管理。
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/admin/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    /** 角色授权页要展示整棵树，所以有角色查看权限也可以调用 */
    @Operation(summary = "全部菜单树")
    @PreAuthorize("@ss.has('system:menu:view') or @ss.has('system:role:view')")
    @GetMapping("/tree")
    public R<List<MenuTreeVO>> tree() {
        return R.ok(menuService.tree());
    }

    @Operation(summary = "菜单详情")
    @PreAuthorize("@ss.has('system:menu:view')")
    @GetMapping("/{id}")
    public R<MenuTreeVO> detail(@PathVariable Long id) {
        return R.ok(menuService.detail(id));
    }

    @Operation(summary = "新增菜单")
    @PreAuthorize("@ss.has('system:menu:edit')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody MenuSaveRequest req) {
        return R.ok(menuService.create(req));
    }

    @Operation(summary = "修改菜单")
    @PreAuthorize("@ss.has('system:menu:edit')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody MenuSaveRequest req) {
        menuService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除菜单")
    @PreAuthorize("@ss.has('system:menu:edit')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok();
    }
}
