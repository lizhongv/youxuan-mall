package com.platform.cloud.mall.trade.pay.controller.app;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.trade.pay.service.CashierService;
import com.platform.cloud.mall.trade.pay.vo.PayChannelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "商城-收银台")
@Validated
@RestController
@RequestMapping("/app/pay")
@RequiredArgsConstructor
public class AppPayController {

    private final CashierService cashierService;

    @Operation(summary = "订单可用的支付方式（后台关闭的不返回，不可用的 enabled=false 并给出原因）")
    @GetMapping("/channels")
    public R<List<PayChannelVO>> channels(@RequestParam @NotNull Long orderId,
                                          @Parameter(description = "是否在微信内置浏览器中")
                                          @RequestParam(defaultValue = "false") boolean inWechat) {
        return R.ok(cashierService.channels(SecurityUtils.currentUserId(), orderId, inWechat));
    }
}
