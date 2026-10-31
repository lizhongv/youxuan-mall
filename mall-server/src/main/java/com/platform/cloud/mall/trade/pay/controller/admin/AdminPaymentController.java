package com.platform.cloud.mall.trade.pay.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.trade.pay.dto.PaymentPageQuery;
import com.platform.cloud.mall.trade.pay.service.PaymentAdminService;
import com.platform.cloud.mall.trade.pay.vo.PaymentAdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-支付流水")
@Validated
@RestController
@RequestMapping("/admin/trade/payment")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentAdminService paymentAdminService;

    @Operation(summary = "支付流水分页")
    @PreAuthorize("@ss.has('trade:payment:view')")
    @GetMapping("/page")
    public R<PageVO<PaymentAdminVO>> page(@Valid PaymentPageQuery query) {
        return R.ok(paymentAdminService.page(query));
    }

    @Operation(summary = "向渠道查单并同步结果（回调丢失、对账时使用）")
    @PreAuthorize("@ss.has('trade:payment:edit')")
    @PostMapping("/{id}/sync")
    public R<PaymentAdminVO> sync(@PathVariable Long id) {
        return R.ok(paymentAdminService.sync(id));
    }
}
