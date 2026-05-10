package com.platform.cloud.order.service;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.order.dto.OrderCreateRequest;
import com.platform.cloud.order.entity.OrderEntity;
import com.platform.cloud.order.entity.OrderItemEntity;
import com.platform.cloud.order.mapper.OrderItemMapper;
import com.platform.cloud.order.mapper.OrderMapper;
import com.platform.cloud.order.mq.OrderCreatedEvent;
import com.platform.cloud.order.mq.OrderMessages;
import com.platform.cloud.order.rpc.GoodsServiceClient;
import com.platform.cloud.order.vo.OrderVO;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final RocketMQTemplate rocketMQ;
    private final GoodsServiceClient goodsClient;

    /**
     * 整个流程包在 @GlobalTransactional 里：
     *   1) order-service 写订单（本地事务，分支 #1）
     *   2) Feign 调 goods-service 扣库存（远程分支 #2，由 Seata 通过 XID 自动加入）
     *   3) 任何一步抛异常 → TC 协调全部分支回滚
     * <p>{@code req.remark} 含 "FAIL_AFTER_RPC" 时模拟扣库存成功后再抛异常 → 验证回滚已扣的库存
     */
    @GlobalTransactional(rollbackFor = Exception.class, name = "create-order-tx", timeoutMills = 60000)
    public OrderVO create(Long userId, OrderCreateRequest req) {
        log.info("Begin global tx, XID={}", RootContext.getXID());
        // 1) 计算总金额
        BigDecimal total = req.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2) 落库订单主表 + 明细
        OrderEntity order = new OrderEntity();
        order.setOrderNo("OD" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setStatus(0); // 待支付
        order.setRemark(req.getRemark());
        orderMapper.insert(order);

        for (OrderCreateRequest.Item it : req.getItems()) {
            OrderItemEntity item = new OrderItemEntity();
            item.setOrderId(order.getId());
            item.setGoodsId(it.getGoodsId());
            item.setSkuId(it.getSkuId());
            item.setGoodsName(it.getGoodsName());
            item.setSkuSpecs(it.getSkuSpecs());
            item.setUnitPrice(it.getUnitPrice());
            item.setQuantity(it.getQuantity());
            item.setSubtotal(it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
            itemMapper.insert(item);
        }

        // 2.5) 同步扣库存（Seata 分支事务 #2，跨 service）
        for (OrderCreateRequest.Item it : req.getItems()) {
            var rpc = goodsClient.decrementStock(it.getSkuId(), it.getQuantity());
            if (rpc == null || rpc.getCode() != 0) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR,
                        "扣库存失败 skuId=" + it.getSkuId() + ": " + (rpc == null ? "null" : rpc.getMsg()));
            }
            log.info("Stock decremented for skuId={} qty={} remaining={}",
                    it.getSkuId(), it.getQuantity(), rpc.getData());
        }

        // 故障注入：演示 Seata 全局回滚
        if (req.getRemark() != null && req.getRemark().contains("FAIL_AFTER_RPC")) {
            log.warn("Injecting failure after stock deducted, expecting Seata rollback...");
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "故意失败（验证 Seata 回滚）");
        }

        // 3) 发 MQ 事件（异步通知库存/优惠券/通知服务）
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .createdAt(LocalDateTime.now())
                .items(req.getItems().stream().map(i ->
                        OrderCreatedEvent.Item.builder()
                                .goodsId(i.getGoodsId())
                                .skuId(i.getSkuId())
                                .skuSpecs(i.getSkuSpecs())
                                .quantity(i.getQuantity())
                                .unitPrice(i.getUnitPrice())
                                .build()).toList())
                .build();

        try {
            // destination = topic:tag
            rocketMQ.send(OrderMessages.TOPIC_ORDER + ":" + OrderMessages.TAG_CREATED,
                    MessageBuilder.withPayload(event)
                            .setHeader("orderId", order.getId())
                            .setHeader("KEYS", order.getOrderNo())   // 业务键，便于 broker 检索
                            .build());
            log.info("Order created and MQ sent: id={}, orderNo={}", order.getId(), order.getOrderNo());
        } catch (Exception e) {
            // 真实场景应：本地事务表 + 定时补偿（保证 at-least-once）
            log.error("Failed to publish order_event:created for order {}", order.getId(), e);
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "MQ 发送失败：" + e.getMessage());
        }

        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setRemark(order.getRemark());
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }

    public OrderVO getById(Long id) {
        OrderEntity e = orderMapper.selectById(id);
        if (e == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在: " + id);
        OrderVO vo = new OrderVO();
        vo.setId(e.getId());
        vo.setOrderNo(e.getOrderNo());
        vo.setUserId(e.getUserId());
        vo.setTotalAmount(e.getTotalAmount());
        vo.setStatus(e.getStatus());
        vo.setRemark(e.getRemark());
        vo.setCreatedAt(e.getCreatedAt());
        return vo;
    }

    public List<OrderVO> recent() {
        List<OrderEntity> rows = orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderEntity>()
                        .orderByDesc(OrderEntity::getId).last("LIMIT 20"));
        return rows.stream().map(e -> {
            OrderVO vo = new OrderVO();
            vo.setId(e.getId());
            vo.setOrderNo(e.getOrderNo());
            vo.setUserId(e.getUserId());
            vo.setTotalAmount(e.getTotalAmount());
            vo.setStatus(e.getStatus());
            vo.setCreatedAt(e.getCreatedAt());
            return vo;
        }).toList();
    }
}
