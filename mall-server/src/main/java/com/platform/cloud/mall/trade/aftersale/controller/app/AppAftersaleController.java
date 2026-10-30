package com.platform.cloud.mall.trade.aftersale.controller.app;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.trade.aftersale.dto.AftersaleApplyRequest;
import com.platform.cloud.mall.trade.aftersale.dto.AftersalePageQuery;
import com.platform.cloud.mall.trade.aftersale.dto.AftersaleReturnRequest;
import com.platform.cloud.mall.trade.aftersale.service.AftersaleService;
import com.platform.cloud.mall.trade.aftersale.vo.AftersaleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商城-售后")
@Validated
@RestController
@RequestMapping("/app/aftersale")
@RequiredArgsConstructor
public class AppAftersaleController {

    private final AftersaleService aftersaleService;

    @Operation(summary = "申请售后")
    @PostMapping
    public R<Long> apply(@Valid @RequestBody AftersaleApplyRequest req) {
        return R.ok(aftersaleService.apply(SecurityUtils.currentUserId(), req));
    }

    @Operation(summary = "我的售后")
    @GetMapping("/page")
    public R<PageVO<AftersaleVO>> page(@Valid AftersalePageQuery query) {
        return R.ok(aftersaleService.memberPage(SecurityUtils.currentUserId(), query));
    }

    @Operation(summary = "售后详情")
    @GetMapping("/{id}")
    public R<AftersaleVO> detail(@PathVariable Long id) {
        return R.ok(aftersaleService.memberDetail(SecurityUtils.currentUserId(), id));
    }

    @Operation(summary = "撤销售后（仅申请中 / 待退货）")
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        aftersaleService.memberCancel(SecurityUtils.currentUserId(), id);
        return R.ok();
    }

    @Operation(summary = "填写退货物流（待退货 → 退货中）")
    @PutMapping("/{id}/return")
    public R<Void> returnGoods(@PathVariable Long id, @Valid @RequestBody AftersaleReturnRequest req) {
        aftersaleService.memberReturn(SecurityUtils.currentUserId(), id, req);
        return R.ok();
    }
}
