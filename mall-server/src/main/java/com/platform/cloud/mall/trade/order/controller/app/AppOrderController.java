package com.platform.cloud.mall.trade.order.controller.app;

import com.platform.cloud.bootstrap.common.idempotent.Idempotent;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.system.support.RequestUtils;
import com.platform.cloud.mall.trade.order.dto.OrderCancelRequest;
import com.platform.cloud.mall.trade.order.dto.OrderPageQuery;
import com.platform.cloud.mall.trade.order.dto.OrderPayRequest;
import com.platform.cloud.mall.trade.order.dto.OrderSubmitRequest;
import com.platform.cloud.mall.trade.order.service.OrderCheckoutService;
import com.platform.cloud.mall.trade.order.service.OrderService;
import com.platform.cloud.mall.trade.order.vo.OrderConfirmVO;
import com.platform.cloud.mall.trade.order.vo.OrderSubmitVO;
import com.platform.cloud.mall.trade.order.vo.OrderVO;
import com.platform.cloud.mall.trade.pay.service.CashierService;
import com.platform.cloud.mall.trade.pay.vo.PayLaunchVO;
import com.platform.cloud.mall.trade.pay.vo.PayStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "商城-订单")
@Validated
@RestController
@RequestMapping("/app/order")
@RequiredArgsConstructor
public class AppOrderController {

    private final OrderCheckoutService checkoutService;
    private final OrderService orderService;
    private final CashierService cashierService;

    @Operation(summary = "结算预览")
    @PostMapping("/confirm")
    public R<OrderConfirmVO> confirm(@Valid @RequestBody OrderSubmitRequest req) {
        return R.ok(checkoutService.confirm(SecurityUtils.currentUserId(), req));
    }

    @Operation(summary = "提交订单（需幂等头 X-Idempotent-Key）")
    @Idempotent(prefix = "idem:order:submit:", ttl = 300)
    @PostMapping
    public R<OrderSubmitVO> submit(@Valid @RequestBody OrderSubmitRequest req) {
        return R.ok(checkoutService.submit(SecurityUtils.currentUserId(), req));
    }

    @Operation(summary = "我的订单")
    @GetMapping("/page")
    public R<PageVO<OrderVO>> page(@Valid OrderPageQuery query) {
        return R.ok(orderService.memberPage(SecurityUtils.currentUserId(), query));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<OrderVO> detail(@PathVariable Long id) {
        return R.ok(orderService.memberDetail(SecurityUtils.currentUserId(), id));
    }

    @Operation(summary = "支付订单（需幂等头 X-Idempotent-Key）：同步渠道返回 paid=true，异步渠道返回 payParams")
    @Idempotent(prefix = "idem:order:pay:", ttl = 300)
    @PostMapping("/{id}/pay")
    public R<PayLaunchVO> pay(@PathVariable Long id, @Valid @RequestBody OrderPayRequest req) {
        return R.ok(cashierService.pay(SecurityUtils.currentUserId(), id, req.getPayType(), RequestUtils.clientIp()));
    }

    @Operation(summary = "支付结果（从渠道返回后轮询；本地未支付时主动向渠道查单）")
    @GetMapping("/{id}/pay-status")
    public R<PayStatusVO> payStatus(@PathVariable Long id) {
        return R.ok(cashierService.payStatus(SecurityUtils.currentUserId(), id));
    }

    @Operation(summary = "取消订单（仅待付款）")
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id, @Valid @RequestBody(required = false) OrderCancelRequest req) {
        orderService.memberCancel(SecurityUtils.currentUserId(), id, req == null ? null : req.getReason());
        return R.ok();
    }

    @Operation(summary = "确认收货（仅待收货）")
    @PutMapping("/{id}/receive")
    public R<Void> receive(@PathVariable Long id) {
        orderService.memberReceive(SecurityUtils.currentUserId(), id);
        return R.ok();
    }

    @Operation(summary = "删除订单（仅已完成 / 已取消，只对买家隐藏）")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        orderService.memberDelete(SecurityUtils.currentUserId(), id);
        return R.ok();
    }
}
