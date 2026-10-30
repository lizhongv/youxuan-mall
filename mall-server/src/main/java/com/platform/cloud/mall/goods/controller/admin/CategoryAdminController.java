package com.platform.cloud.mall.goods.controller.admin;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.goods.dto.CategoryRequest;
import com.platform.cloud.mall.goods.service.CategoryService;
import com.platform.cloud.mall.goods.vo.CategoryVO;
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
 * 管理端：商品类目。
 */
@Tag(name = "管理端-商品类目")
@RestController
@RequestMapping("/admin/goods/category")
@RequiredArgsConstructor
public class CategoryAdminController {

    private final CategoryService categoryService;

    /** 商品编辑页也要选类目，所以有商品权限也可以查看类目树 */
    @Operation(summary = "类目树（含停用）")
    @GetMapping("/tree")
    @PreAuthorize("@ss.has('goods:category:view') or @ss.has('goods:goods:view')")
    public R<List<CategoryVO>> tree() {
        return R.ok(categoryService.adminTree());
    }

    @Operation(summary = "新增类目")
    @PostMapping
    @PreAuthorize("@ss.has('goods:category:edit')")
    public R<Long> create(@Valid @RequestBody CategoryRequest req) {
        return R.ok(categoryService.create(req));
    }

    @Operation(summary = "修改类目")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.has('goods:category:edit')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        categoryService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除类目（有子类目或商品时不能删）")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.has('goods:category:edit')")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}
