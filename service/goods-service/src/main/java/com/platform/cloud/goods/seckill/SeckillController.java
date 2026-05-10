package com.platform.cloud.goods.seckill;

import com.platform.cloud.bootstrap.common.web.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Seckill", description = "秒杀：Redis Lua 原子扣减")
@RestController
@RequestMapping("/api/goods/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService service;

    @Operation(summary = "[管理] 把 DB SKU 当前库存预热到 Redis",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/preload/{skuId}")
    public R<Integer> preload(@PathVariable Long skuId) {
        return R.ok(service.preload(skuId));
    }

    @Operation(summary = "秒杀下单（Redis Lua 原子扣库存）",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PostMapping("/{skuId}")
    public R<Map<String, Object>> buy(@PathVariable Long skuId,
                                       @RequestParam(defaultValue = "1") int qty) {
        long remaining = service.buy(skuId, qty);
        return R.ok(Map.of(
                "skuId", skuId,
                "qty", qty,
                "remainingStock", remaining
        ));
    }

    @Operation(summary = "查 Redis 当前库存 + 已售")
    @GetMapping("/{skuId}/stat")
    public R<Map<String, Object>> stat(@PathVariable Long skuId) {
        return R.ok(Map.of(
                "remainingStock", service.currentStock(skuId),
                "sold", service.currentSold(skuId)
        ));
    }
}
