package com.platform.cloud.mall.trade.aftersale.enums;

import com.platform.cloud.bootstrap.common.exception.BusinessException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 售后状态及允许的流转，与 api.md §3 一致。
 * <pre>
 * APPLYING ─同意(仅退款)─▶ REFUNDED
 * APPLYING ─同意(退货退款)─▶ WAIT_RETURN ─填物流─▶ RETURNING ─确认收货─▶ REFUNDED
 * APPLYING ─拒绝─▶ REJECTED        APPLYING / WAIT_RETURN ─撤销─▶ CANCELLED
 * </pre>
 */
public enum AftersaleStatus {

    APPLYING("申请中"),
    WAIT_RETURN("待退货"),
    RETURNING("退货中"),
    REFUNDED("退款完成"),
    REJECTED("已拒绝"),
    CANCELLED("已撤销");

    /** 进行中的状态名：同一明细同时只能有一个，且占用退款额度 */
    public static final List<String> IN_PROGRESS = List.of(APPLYING.name(), WAIT_RETURN.name(), RETURNING.name());

    private final String text;

    AftersaleStatus(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }

    public Set<AftersaleStatus> nextStatuses() {
        return switch (this) {
            case APPLYING -> EnumSet.of(REFUNDED, WAIT_RETURN, REJECTED, CANCELLED);
            case WAIT_RETURN -> EnumSet.of(RETURNING, CANCELLED);
            case RETURNING -> EnumSet.of(REFUNDED);
            case REFUNDED, REJECTED, CANCELLED -> Collections.emptySet();
        };
    }

    public boolean canTransitTo(AftersaleStatus target) {
        return target != null && nextStatuses().contains(target);
    }

    public static AftersaleStatus of(String name) {
        for (AftersaleStatus s : values()) {
            if (s.name().equals(name)) {
                return s;
            }
        }
        throw new BusinessException("未知售后状态：" + name);
    }

    public static String textOf(String name) {
        for (AftersaleStatus s : values()) {
            if (s.name().equals(name)) {
                return s.text;
            }
        }
        return name;
    }
}
