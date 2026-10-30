package com.platform.cloud.mall.goods.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.goods.dto.GoodsFlagsRequest;
import com.platform.cloud.mall.goods.dto.GoodsForm;
import com.platform.cloud.mall.goods.dto.GoodsQuery;
import com.platform.cloud.mall.goods.dto.GoodsStatusRequest;
import com.platform.cloud.mall.goods.service.GoodsService;
import com.platform.cloud.mall.goods.vo.GoodsAdminItemVO;
import com.platform.cloud.mall.goods.vo.GoodsDetailVO;
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

/**
 * 管理端：商品。
 */
@Tag(name = "管理端-商品")
@RestController
@RequestMapping("/admin/goods/goods")
@RequiredArgsConstructor
public class GoodsAdminController {

    private final GoodsService goodsService;

    @Operation(summary = "商品分页")
    @GetMapping("/page")
    @PreAuthorize("@ss.has('goods:goods:view')")
    public R<PageVO<GoodsAdminItemVO>> page(@Valid GoodsQuery q) {
        return R.ok(goodsService.page(q));
    }

    @Operation(summary = "商品详情（编辑回显）")
    @GetMapping("/{id}")
    @PreAuthorize("@ss.has('goods:goods:view')")
    public R<GoodsDetailVO> detail(@PathVariable Long id) {
        return R.ok(goodsService.detail(id));
    }

    @Operation(summary = "新增商品")
    @PostMapping
    @PreAuthorize("@ss.has('goods:goods:edit')")
    public R<Long> create(@Valid @RequestBody GoodsForm form) {
        return R.ok(goodsService.create(form));
    }

    @Operation(summary = "修改商品")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.has('goods:goods:edit')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody GoodsForm form) {
        goodsService.update(id, form);
        return R.ok();
    }

    @Operation(summary = "删除商品（需先下架）")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.has('goods:goods:edit')")
    public R<Void> delete(@PathVariable Long id) {
        goodsService.delete(id);
        return R.ok();
    }

    @Operation(summary = "批量上下架")
    @PutMapping("/status")
    @PreAuthorize("@ss.has('goods:goods:edit')")
    public R<Void> updateStatus(@Valid @RequestBody GoodsStatusRequest req) {
        goodsService.updateStatus(req);
        return R.ok();
    }

    @Operation(summary = "设置新品 / 热卖 / 推荐标签")
    @PutMapping("/{id}/flags")
    @PreAuthorize("@ss.has('goods:goods:edit')")
    public R<Void> updateFlags(@PathVariable Long id, @Valid @RequestBody GoodsFlagsRequest req) {
        goodsService.updateFlags(id, req);
        return R.ok();
    }
}
