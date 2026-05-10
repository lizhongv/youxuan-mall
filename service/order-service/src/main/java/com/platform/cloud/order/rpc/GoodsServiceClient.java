package com.platform.cloud.order.rpc;

import com.platform.cloud.bootstrap.common.web.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 调 goods-service 的 Feign 客户端。Seata AT 模式下，Feign 会自动透传 XID（通过 SeataFeignRequestInterceptor）。
 */
@FeignClient(name = "goods-service", contextId = "goodsServiceClient")
public interface GoodsServiceClient {

    @PostMapping("/api/goods/_internal/sku/{skuId}/decrement")
    R<Integer> decrementStock(@PathVariable("skuId") Long skuId,
                              @RequestParam("quantity") Integer quantity);
}
