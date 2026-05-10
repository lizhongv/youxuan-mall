package com.platform.cloud.order.web;

import com.platform.cloud.bootstrap.common.idempotent.Idempotent;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.order.dto.OrderCreateRequest;
import com.platform.cloud.order.service.OrderService;
import com.platform.cloud.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Order", description = "订单：下单异步驱动后续流程")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @Operation(summary = "创建订单（同步落库 + 异步发 MQ）",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @Idempotent(prefix = "order:create:", ttl = 60)
    @PostMapping
    public R<OrderVO> create(Authentication auth, @Valid @RequestBody OrderCreateRequest req) {
        // Phase 3.3 演示分片：用 username 长度（admin=5→orders_1, user=4→orders_0）
        // Phase 1.4 接 DB 后改用真实 user.id
        Long userId = (long) auth.getName().length();
        return R.ok(service.create(userId, req));
    }

    @Operation(summary = "订单详情", security = {@SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/{id}")
    public R<OrderVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    @Operation(summary = "最近订单（演示用）", security = {@SecurityRequirement(name = "bearerAuth")})
    @GetMapping
    public R<List<OrderVO>> recent() {
        return R.ok(service.recent());
    }
}
