package com.platform.cloud.mall.marketing.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.marketing.dto.BannerQuery;
import com.platform.cloud.mall.marketing.dto.BannerRequest;
import com.platform.cloud.mall.marketing.service.BannerService;
import com.platform.cloud.mall.marketing.vo.BannerVO;
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
 * 管理端：轮播广告。
 */
@Tag(name = "管理端-轮播广告")
@RestController
@RequestMapping("/admin/marketing/banner")
@RequiredArgsConstructor
public class BannerAdminController {

    private final BannerService bannerService;

    @Operation(summary = "轮播分页")
    @GetMapping("/page")
    @PreAuthorize("@ss.has('marketing:banner:view')")
    public R<PageVO<BannerVO>> page(@Valid BannerQuery q) {
        return R.ok(bannerService.page(q));
    }

    @Operation(summary = "轮播详情")
    @GetMapping("/{id}")
    @PreAuthorize("@ss.has('marketing:banner:view')")
    public R<BannerVO> get(@PathVariable Long id) {
        return R.ok(bannerService.get(id));
    }

    @Operation(summary = "新增轮播")
    @PostMapping
    @PreAuthorize("@ss.has('marketing:banner:edit')")
    public R<Long> create(@Valid @RequestBody BannerRequest req) {
        return R.ok(bannerService.create(req));
    }

    @Operation(summary = "修改轮播")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.has('marketing:banner:edit')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody BannerRequest req) {
        bannerService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除轮播")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.has('marketing:banner:edit')")
    public R<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return R.ok();
    }
}
