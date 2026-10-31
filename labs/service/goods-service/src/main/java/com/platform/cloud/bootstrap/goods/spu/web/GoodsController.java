package com.platform.cloud.bootstrap.goods.spu.web;

import com.platform.cloud.bootstrap.common.idempotent.Idempotent;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.bootstrap.goods.spu.dto.GoodsCreateRequest;
import com.platform.cloud.bootstrap.goods.spu.dto.GoodsQuery;
import com.platform.cloud.bootstrap.goods.spu.dto.GoodsUpdateRequest;
import com.platform.cloud.bootstrap.goods.spu.dto.SkuUpsertRequest;
import com.platform.cloud.bootstrap.goods.spu.service.GoodsService;
import com.platform.cloud.bootstrap.goods.spu.vo.GoodsDetailVO;
import com.platform.cloud.bootstrap.goods.spu.vo.GoodsVO;
import com.platform.cloud.bootstrap.goods.spu.vo.SkuVO;
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

@Tag(name = "Goods", description = "商品 SPU + SKU")
@Validated
@RestController
@RequestMapping("/api/goods/items")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService service;

    // ========== SPU ==========

    @Operation(summary = "分页列表（不含 SKU）")
    @GetMapping
    public R<PageVO<GoodsVO>> page(@Valid @ModelAttribute GoodsQuery query) {
        return R.ok(service.page(query));
    }

    @Operation(summary = "详情（含 SKU）")
    @GetMapping("/{id}")
    public R<GoodsDetailVO> detail(@PathVariable Long id) {
        return R.ok(service.detail(id));
    }

    @Operation(summary = "创建商品（SPU + SKU）", security = {@SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent(prefix = "goods:create:", ttl = 60)
    @PostMapping
    public R<Long> create(@Valid @RequestBody GoodsCreateRequest req) {
        return R.ok(service.create(req));
    }

    @Operation(summary = "更新 SPU 字段", security = {@SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody GoodsUpdateRequest req) {
        service.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除（软删，级联 SKU；上架中拒绝）", security = {@SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @Operation(summary = "上架（DRAFT/OFF_SALE → ON_SALE）", security = {@SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        service.publish(id);
        return R.ok();
    }

    @Operation(summary = "下架（ON_SALE → OFF_SALE）", security = {@SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/unpublish")
    public R<Void> unpublish(@PathVariable Long id) {
        service.unpublish(id);
        return R.ok();
    }

    // ========== SKU ==========

    @Operation(summary = "新增 SKU", security = {@SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{goodsId}/skus")
    public R<Long> addSku(@PathVariable Long goodsId, @Valid @RequestBody SkuUpsertRequest req) {
        return R.ok(service.addSku(goodsId, req));
    }

    @Operation(summary = "更新 SKU", security = {@SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{goodsId}/skus/{skuId}")
    public R<Void> updateSku(@PathVariable Long goodsId, @PathVariable Long skuId,
                             @Valid @RequestBody SkuUpsertRequest req) {
        service.updateSku(goodsId, skuId, req);
        return R.ok();
    }

    @Operation(summary = "删除 SKU（保留 ≥ 1 条）", security = {@SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{goodsId}/skus/{skuId}")
    public R<Void> deleteSku(@PathVariable Long goodsId, @PathVariable Long skuId) {
        service.deleteSku(goodsId, skuId);
        return R.ok();
    }

    @Operation(summary = "SKU 详情")
    @GetMapping("/{goodsId}/skus/{skuId}")
    public R<SkuVO> getSku(@PathVariable Long goodsId, @PathVariable Long skuId) {
        return R.ok(service.getSku(goodsId, skuId));
    }
}
