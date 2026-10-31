package com.platform.cloud.bootstrap.goods.category.web;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.bootstrap.goods.category.dto.CategoryUpsertRequest;
import com.platform.cloud.bootstrap.goods.category.service.CategoryService;
import com.platform.cloud.bootstrap.goods.category.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Category", description = "商品分类")
@Validated
@RestController
@RequestMapping("/api/goods/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @Operation(summary = "分页列表")
    @GetMapping
    public R<PageVO<CategoryVO>> page(@Valid @ModelAttribute PageQuery query) {
        return R.ok(service.page(query));
    }

    @Operation(summary = "树形结构")
    @GetMapping("/tree")
    public R<List<CategoryVO>> tree() {
        return R.ok(service.tree());
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public R<CategoryVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "创建", security = {@SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody CategoryUpsertRequest req) {
        return R.ok(service.create(req));
    }

    @Operation(summary = "更新", security = {@SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryUpsertRequest req) {
        service.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除（软删）", security = {@SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}
