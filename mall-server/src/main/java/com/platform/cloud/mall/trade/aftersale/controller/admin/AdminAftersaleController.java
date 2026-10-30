package com.platform.cloud.mall.trade.aftersale.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.trade.aftersale.dto.AftersaleAuditRequest;
import com.platform.cloud.mall.trade.aftersale.dto.AftersalePageQuery;
import com.platform.cloud.mall.trade.aftersale.service.AftersaleService;
import com.platform.cloud.mall.trade.aftersale.vo.AftersaleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-售后")
@Validated
@RestController
@RequestMapping("/admin/trade/aftersale")
@RequiredArgsConstructor
public class AdminAftersaleController {

    private final AftersaleService aftersaleService;

    @Operation(summary = "售后分页")
    @PreAuthorize("@ss.has('trade:aftersale:view')")
    @GetMapping("/page")
    public R<PageVO<AftersaleVO>> page(@Valid AftersalePageQuery query) {
        return R.ok(aftersaleService.adminPage(query));
    }

    @Operation(summary = "售后详情（含订单明细快照）")
    @PreAuthorize("@ss.has('trade:aftersale:view')")
    @GetMapping("/{id}")
    public R<AftersaleVO> detail(@PathVariable Long id) {
        return R.ok(aftersaleService.adminDetail(id));
    }

    @Operation(summary = "同意（仅退款直接退款；退货退款进入待退货）")
    @PreAuthorize("@ss.has('trade:aftersale:audit')")
    @PutMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id, @Valid @RequestBody(required = false) AftersaleAuditRequest req) {
        aftersaleService.approve(SecurityUtils.currentUserId(), id, req == null ? null : req.getRemark());
        return R.ok();
    }

    @Operation(summary = "拒绝")
    @PreAuthorize("@ss.has('trade:aftersale:audit')")
    @PutMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @Valid @RequestBody(required = false) AftersaleAuditRequest req) {
        aftersaleService.reject(SecurityUtils.currentUserId(), id, req == null ? null : req.getRemark());
        return R.ok();
    }

    @Operation(summary = "确认收到退货并退款（退货中 → 退款完成）")
    @PreAuthorize("@ss.has('trade:aftersale:audit')")
    @PutMapping("/{id}/receive")
    public R<Void> receive(@PathVariable Long id) {
        aftersaleService.receiveReturn(SecurityUtils.currentUserId(), id);
        return R.ok();
    }
}
