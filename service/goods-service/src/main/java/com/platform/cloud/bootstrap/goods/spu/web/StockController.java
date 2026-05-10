package com.platform.cloud.bootstrap.goods.spu.web;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.bootstrap.goods.spu.entity.SkuEntity;
import com.platform.cloud.bootstrap.goods.spu.mapper.SkuMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 2.6 演示：同步扣库存接口（下游被 order-service 通过 Feign 调用）。
 * 这是 Seata AT 模式 RM（Resource Manager）的"分支事务"。
 */
@Slf4j
@Tag(name = "Stock", description = "[内部 RPC] 同步扣库存（Seata AT 分支）")
@RestController
@RequestMapping("/api/goods/_internal/sku")
@RequiredArgsConstructor
public class StockController {

    private final SkuMapper skuMapper;

    @Operation(summary = "扣减 SKU 库存（不足抛业务异常 → Seata 全局回滚）",
            security = {@SecurityRequirement(name = "bearerAuth")})
    @PostMapping("/{skuId}/decrement")
    @Transactional
    public R<Integer> decrement(@PathVariable Long skuId,
                                @RequestParam(defaultValue = "1") Integer quantity) {
        SkuEntity sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "SKU 不存在: " + skuId);
        }
        if (sku.getStock() < quantity) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "库存不足: skuId=" + skuId + " 现有=" + sku.getStock() + " 需要=" + quantity);
        }
        // CAS 扣减：避免并发覆盖
        int affected = skuMapper.update(null,
                new LambdaUpdateWrapper<SkuEntity>()
                        .eq(SkuEntity::getId, skuId)
                        .ge(SkuEntity::getStock, quantity)
                        .setSql("stock = stock - " + quantity));
        if (affected == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "库存扣减失败（并发冲突）: " + skuId);
        }
        int remaining = sku.getStock() - quantity;
        log.info("Stock decremented: skuId={} qty={} remaining={} (XID={})",
                skuId, quantity, remaining,
                io.seata.core.context.RootContext.getXID());
        return R.ok(remaining);
    }
}
