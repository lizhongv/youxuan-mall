package com.platform.cloud.bootstrap.goods.spu;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * 商品状态机。
 * <pre>
 *   DRAFT ──(publish)──► ON_SALE ──(unpublish)──► OFF_SALE
 *                            ▲                       │
 *                            └──────(publish)────────┘
 * </pre>
 */
@Getter
public enum GoodsStatus {

    DRAFT(0),
    ON_SALE(1),
    OFF_SALE(2);

    @EnumValue
    @JsonValue
    private final int code;

    GoodsStatus(int code) {
        this.code = code;
    }

    public static GoodsStatus of(Integer code) {
        if (code == null) return null;
        for (GoodsStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知商品状态: " + code);
    }

    private static final Map<GoodsStatus, Set<GoodsStatus>> TRANSITIONS = Map.of(
            DRAFT, Set.of(ON_SALE),
            ON_SALE, Set.of(OFF_SALE),
            OFF_SALE, Set.of(ON_SALE)
    );

    public boolean canTransitionTo(GoodsStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
