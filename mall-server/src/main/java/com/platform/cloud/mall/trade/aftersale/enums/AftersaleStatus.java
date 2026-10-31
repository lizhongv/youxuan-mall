package com.platform.cloud.mall.trade.aftersale.enums;

import com.platform.cloud.bootstrap.common.exception.BusinessException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 售后状态及允许的流转，与 api.md §3 一致。
 * <pre>
 * APPLYING ─同意(仅退款)─▶ REFUNDING ─渠道退款成功─▶ REFUNDED
 * APPLYING ─同意(退货退款)─▶ WAIT_RETURN ─填物流─▶ RETURNING ─确认收货─▶ REFUNDING ─▶ REFUNDED
 * APPLYING ─拒绝─▶ REJECTED        APPLYING / WAIT_RETURN ─撤销─▶ CANCELLED
 * </pre>
 * REFUNDING：已向渠道发起退款、等待结果。余额 / 模拟支付同一事务内直接到 REFUNDED，
 * 微信退款通常异步到账，由退款回调或补偿查询推进到 REFUNDED。
 */
public enum AftersaleStatus {

    APPLYING("申请中"),
    WAIT_RETURN("待退货"),
    RETURNING("退货中"),
    REFUNDING("退款中"),
    REFUNDED("退款完成"),
    REJECTED("已拒绝"),
    CANCELLED("已撤销");

    /** 进行中的状态名：同一明细同时只能有一个，且占用退款额度 */
    public static final List<String> IN_PROGRESS = List.of(APPLYING.name(), WAIT_RETURN.name(), RETURNING.name(),
            REFUNDING.name());

    private final String text;

    AftersaleStatus(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }

    public Set<AftersaleStatus> nextStatuses() {
        return switch (this) {
            case APPLYING -> EnumSet.of(REFUNDING, WAIT_RETURN, REJECTED, CANCELLED);
            case WAIT_RETURN -> EnumSet.of(RETURNING, CANCELLED);
            case RETURNING -> EnumSet.of(REFUNDING);
            // 渠道退款失败时保持 REFUNDING 并在备注说明，由商户平台人工处理后再走回调 / 查单推进
            case REFUNDING -> EnumSet.of(REFUNDED);
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
