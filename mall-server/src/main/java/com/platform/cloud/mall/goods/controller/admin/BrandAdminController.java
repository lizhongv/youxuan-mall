package com.platform.cloud.mall.goods.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.goods.dto.BrandQuery;
import com.platform.cloud.mall.goods.dto.BrandRequest;
import com.platform.cloud.mall.goods.service.BrandService;
import com.platform.cloud.mall.goods.vo.BrandVO;
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
 * 管理端：品牌。
 */
@Tag(name = "管理端-品牌")
@RestController
@RequestMapping("/admin/goods/brand")
@RequiredArgsConstructor
public class BrandAdminController {

    private final BrandService brandService;

    @Operation(summary = "品牌分页")
    @GetMapping("/page")
    @PreAuthorize("@ss.has('goods:brand:view')")
    public R<PageVO<BrandVO>> page(@Valid BrandQuery q) {
        return R.ok(brandService.page(q));
    }

    /** 商品编辑页的品牌下拉也用这个接口，所以有商品权限也可以查看 */
    @Operation(summary = "全部启用品牌")
    @GetMapping("/list")
    @PreAuthorize("@ss.has('goods:brand:view') or @ss.has('goods:goods:view')")
    public R<List<BrandVO>> list() {
        return R.ok(brandService.listEnabled());
    }

    @Operation(summary = "品牌详情")
    @GetMapping("/{id}")
    @PreAuthorize("@ss.has('goods:brand:view')")
    public R<BrandVO> get(@PathVariable Long id) {
        return R.ok(brandService.get(id));
    }

    @Operation(summary = "新增品牌")
    @PostMapping
    @PreAuthorize("@ss.has('goods:brand:edit')")
    public R<Long> create(@Valid @RequestBody BrandRequest req) {
        return R.ok(brandService.create(req));
    }

    @Operation(summary = "修改品牌")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.has('goods:brand:edit')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody BrandRequest req) {
        brandService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除品牌")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.has('goods:brand:edit')")
    public R<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return R.ok();
    }
}
