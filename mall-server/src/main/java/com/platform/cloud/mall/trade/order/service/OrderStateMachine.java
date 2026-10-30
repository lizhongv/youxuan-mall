package com.platform.cloud.mall.trade.order.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.entity.OrderLog;
import com.platform.cloud.mall.trade.order.enums.OrderStatus;
import com.platform.cloud.mall.trade.order.mapper.OrderLogMapper;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 订单状态变更的唯一入口。
 * <p>
 * 用 {@code UPDATE ... WHERE id = ? AND status = <原状态>} 的条件更新代替乐观锁版本号：
 * 支付回调、超时关单、买家取消可能同时作用于同一订单，只有第一个能更新成功，其余影响 0 行直接失败，
 * 从而杜绝「已支付的订单又被关单」这类资金事故。每次成功变更都写一条 trd_order_log。
 */
@Component
@RequiredArgsConstructor
public class OrderStateMachine {

    private final OrderMapper orderMapper;
    private final OrderLogMapper orderLogMapper;

    /**
     * 执行状态流转。
     *
     * @param order    当前订单（status 为读取时的状态，成功后会被改为目标状态）
     * @param extraSet 同一条 UPDATE 里需要一起写入的字段（如支付时间、发货单号），可为 null
     */
    public void transit(Order order, OrderStatus to, String operator, String remark,
                        Consumer<LambdaUpdateWrapper<Order>> extraSet) {
        OrderStatus from = OrderStatus.of(order.getStatus());
        if (!from.canTransitTo(to)) {
            throw new BusinessException("订单" + from.text() + "，不能变更为" + to.text());
        }
        LambdaUpdateWrapper<Order> w = Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, from.name())
                .set(Order::getStatus, to.name());
        if (extraSet != null) {
            extraSet.accept(w);
        }
        int rows = orderMapper.update(null, w);
        if (rows == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        order.setStatus(to.name());
        log(order.getId(), from.name(), to.name(), operator, remark);
    }

    /** 记录不改变状态的操作（如下单、改价），from 为空表示新建 */
    public void log(Long orderId, String from, String to, String operator, String remark) {
        OrderLog log = new OrderLog();
        log.setOrderId(orderId);
        log.setFromStatus(from == null ? "" : from);
        log.setToStatus(to);
        log.setOperator(operator);
        log.setRemark(remark == null ? "" : truncate(remark));
        orderLogMapper.insert(log);
    }

    /** remark 列 255 字符，超长截断而不是让整个业务失败 */
    private static String truncate(String s) {
        return s.length() <= 255 ? s : s.substring(0, 255);
    }
}
