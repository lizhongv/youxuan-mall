package com.platform.cloud.mall.marketing.api;

import com.platform.cloud.mall.goods.api.GoodsApi.GoodsLine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券对交易领域开放的能力：结算页可用券、计算优惠、核销与退还。
 */
public interface CouponApi {

    /**
     * 结算页列出会员所有未使用且在有效期内的券，并标出对本次商品是否可用、可减多少。
     * 可用的排在前面，按优惠金额从大到小。
     */
    List<CouponOption> listForCheckout(Long memberId, List<GoodsLine> lines);

    /**
     * 校验并计算某张券对本次商品的优惠金额；券不属于该会员、已使用、过期、不满足门槛或范围时抛业务异常。
     * 优惠金额不会超过适用商品金额。
     */
    BigDecimal calculate(Long memberId, Long memberCouponId, List<GoodsLine> lines);

    /** 核销：状态 UNUSED → USED，并累加模板 used；用条件更新防止同一张券被两个订单同时使用 */
    void use(Long memberId, Long memberCouponId, Long orderId);

    /** 退还：订单取消或关单时 USED → UNUSED（若已过期则置为 EXPIRED） */
    void giveBack(Long memberCouponId);

    /** 会员中心角标：未使用且未过期的券数量 */
    long countUsable(Long memberId);

    record CouponOption(Long memberCouponId, String name, String type, BigDecimal threshold, BigDecimal amount,
                        BigDecimal discount, LocalDateTime endAt, boolean usable, BigDecimal discountAmount,
                        String unusableReason) {
    }
}
