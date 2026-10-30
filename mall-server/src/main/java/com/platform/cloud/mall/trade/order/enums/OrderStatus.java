package com.platform.cloud.mall.trade.order.enums;

import com.platform.cloud.bootstrap.common.exception.BusinessException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * 订单状态及允许的流转。所有状态变化都必须先经过 {@link #canTransitTo} 校验，
 * 把「从哪能到哪」集中在一处，避免各业务方法各自判断出现遗漏。
 * <pre>
 * PENDING_PAY ──支付──▶ PENDING_DELIVERY ──发货──▶ DELIVERED ──收货──▶ COMPLETED
 *      └──取消 / 关单──▶ CANCELLED ◀──全部明细退款── PENDING_DELIVERY
 * </pre>
 */
public enum OrderStatus {

    PENDING_PAY("待付款"),
    PENDING_DELIVERY("待发货"),
    DELIVERED("待收货"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String text;

    OrderStatus(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }

    /** 允许流转到的下一状态；终态返回空集 */
    public Set<OrderStatus> nextStatuses() {
        return switch (this) {
            case PENDING_PAY -> EnumSet.of(PENDING_DELIVERY, CANCELLED);
            // 待发货时全部明细退款完成，订单直接取消
            case PENDING_DELIVERY -> EnumSet.of(DELIVERED, CANCELLED);
            case DELIVERED -> EnumSet.of(COMPLETED);
            case COMPLETED, CANCELLED -> Collections.emptySet();
        };
    }

    public boolean canTransitTo(OrderStatus target) {
        return target != null && nextStatuses().contains(target);
    }

    /** 数据库值转枚举；未知值说明数据被手工改坏，直接报错而不是静默忽略 */
    public static OrderStatus of(String name) {
        for (OrderStatus s : values()) {
            if (s.name().equals(name)) {
                return s;
            }
        }
        throw new BusinessException("未知订单状态：" + name);
    }

    /** 列表展示用，未知值原样返回，不让一条脏数据拖垮整个列表 */
    public static String textOf(String name) {
        for (OrderStatus s : values()) {
            if (s.name().equals(name)) {
                return s.text;
            }
        }
        return name;
    }
}
