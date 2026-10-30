package com.platform.cloud.mall.trade.order.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.trade.order.dto.AdminOrderPageQuery;
import com.platform.cloud.mall.trade.order.dto.OrderCancelRequest;
import com.platform.cloud.mall.trade.order.dto.OrderDeliverRequest;
import com.platform.cloud.mall.trade.order.dto.OrderPriceRequest;
import com.platform.cloud.mall.trade.order.dto.OrderRemarkRequest;
import com.platform.cloud.mall.trade.order.service.OrderService;
import com.platform.cloud.mall.trade.order.vo.OrderVO;
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

import java.util.Map;

@Tag(name = "管理端-订单")
@Validated
@RestController
@RequestMapping("/admin/trade/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "订单分页")
    @PreAuthorize("@ss.has('trade:order:view')")
    @GetMapping("/page")
    public R<PageVO<OrderVO>> page(@Valid AdminOrderPageQuery query) {
        return R.ok(orderService.adminPage(query));
    }

    @Operation(summary = "各状态订单数")
    @PreAuthorize("@ss.has('trade:order:view')")
    @GetMapping("/status-count")
    public R<Map<String, Long>> statusCount() {
        return R.ok(orderService.statusCount());
    }

    @Operation(summary = "订单详情")
    @PreAuthorize("@ss.has('trade:order:view')")
    @GetMapping("/{id}")
    public R<OrderVO> detail(@PathVariable Long id) {
        return R.ok(orderService.adminDetail(id));
    }

    @Operation(summary = "发货（仅待发货）")
    @PreAuthorize("@ss.has('trade:order:deliver')")
    @PutMapping("/{id}/deliver")
    public R<Void> deliver(@PathVariable Long id, @Valid @RequestBody OrderDeliverRequest req) {
        orderService.deliver(SecurityUtils.currentUserId(), id, req);
        return R.ok();
    }

    @Operation(summary = "改价（仅待付款，实付不低于 0.01）")
    @PreAuthorize("@ss.has('trade:order:edit')")
    @PutMapping("/{id}/price")
    public R<Void> price(@PathVariable Long id, @Valid @RequestBody OrderPriceRequest req) {
        orderService.adjustPrice(SecurityUtils.currentUserId(), id, req.getAdjustAmount());
        return R.ok();
    }

    @Operation(summary = "商家备注")
    @PreAuthorize("@ss.has('trade:order:edit')")
    @PutMapping("/{id}/remark")
    public R<Void> remark(@PathVariable Long id, @Valid @RequestBody OrderRemarkRequest req) {
        orderService.updateAdminRemark(SecurityUtils.currentUserId(), id, req.getAdminRemark());
        return R.ok();
    }

    @Operation(summary = "取消订单（仅待付款）")
    @PreAuthorize("@ss.has('trade:order:edit')")
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id, @Valid @RequestBody(required = false) OrderCancelRequest req) {
        orderService.adminCancel(SecurityUtils.currentUserId(), id, req == null ? null : req.getReason());
        return R.ok();
    }
}
