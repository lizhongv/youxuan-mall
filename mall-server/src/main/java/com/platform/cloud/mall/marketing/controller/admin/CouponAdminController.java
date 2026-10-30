package com.platform.cloud.mall.marketing.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.marketing.dto.CouponQuery;
import com.platform.cloud.mall.marketing.dto.CouponRecordQuery;
import com.platform.cloud.mall.marketing.dto.CouponRequest;
import com.platform.cloud.mall.marketing.service.CouponService;
import com.platform.cloud.mall.marketing.vo.CouponRecordVO;
import com.platform.cloud.mall.marketing.vo.CouponVO;
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
 * 管理端：优惠券。
 */
@Tag(name = "管理端-优惠券")
@RestController
@RequestMapping("/admin/marketing/coupon")
@RequiredArgsConstructor
public class CouponAdminController {

    private final CouponService couponService;

    @Operation(summary = "优惠券分页")
    @GetMapping("/page")
    @PreAuthorize("@ss.has('marketing:coupon:view')")
    public R<PageVO<CouponVO>> page(@Valid CouponQuery q) {
        return R.ok(couponService.page(q));
    }

    @Operation(summary = "优惠券详情")
    @GetMapping("/{id}")
    @PreAuthorize("@ss.has('marketing:coupon:view')")
    public R<CouponVO> get(@PathVariable Long id) {
        return R.ok(couponService.get(id));
    }

    @Operation(summary = "新增优惠券")
    @PostMapping
    @PreAuthorize("@ss.has('marketing:coupon:edit')")
    public R<Long> create(@Valid @RequestBody CouponRequest req) {
        return R.ok(couponService.create(req));
    }

    @Operation(summary = "修改优惠券")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.has('marketing:coupon:edit')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CouponRequest req) {
        couponService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除优惠券（已有领取时不能删）")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.has('marketing:coupon:edit')")
    public R<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return R.ok();
    }

    @Operation(summary = "启用 / 停发")
    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.has('marketing:coupon:edit')")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        couponService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "领取记录")
    @GetMapping("/{id}/records")
    @PreAuthorize("@ss.has('marketing:coupon:view')")
    public R<PageVO<CouponRecordVO>> records(@PathVariable Long id, @Valid CouponRecordQuery q) {
        return R.ok(couponService.records(id, q));
    }
}
