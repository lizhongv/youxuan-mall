package com.platform.cloud.goods.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 监听订单创建事件 → 模拟扣库存。
 * 真实实现：调 stock-service Feign / Redis Lua 预扣 / DB 乐观锁。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "order_event",
        consumerGroup = "goods-service-stock-consumer",
        selectorExpression = "created"
)
public class OrderCreatedListener implements RocketMQListener<OrderCreatedDto> {

    @Override
    public void onMessage(OrderCreatedDto event) {
        log.info("[MQ] 收到订单创建事件 orderNo={} userId={} totalAmount={} 件数={}",
                event.getOrderNo(), event.getUserId(), event.getTotalAmount(),
                event.getItems() == null ? 0 : event.getItems().size());
        if (event.getItems() != null) {
            for (OrderCreatedDto.Item it : event.getItems()) {
                log.info("[MQ]   → 模拟扣库存: skuId={} 数量={} 规格={}",
                        it.getSkuId(), it.getQuantity(), it.getSkuSpecs());
            }
        }
    }
}
