package com.platform.cloud.mall.marketing.support;

import com.platform.cloud.mall.goods.api.GoodsApi.GoodsLine;
import com.platform.cloud.mall.marketing.entity.Coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 优惠券金额计算（纯函数，不查库，便于单测穷举边界）。
 * <ol>
 *   <li>按适用范围筛出适用商品行，求和得到「适用金额」（调用方传入的是会员折扣后的金额）；</li>
 *   <li>满减 / 折扣券：门槛 &gt; 0 时要求适用金额 ≥ 门槛；无门槛券忽略门槛；</li>
 *   <li>满减、无门槛减 amount；折扣减 适用金额 × (1 - discount)，maxAmount &gt; 0 时封顶；</li>
 *   <li>优惠不超过适用金额，结果两位小数 HALF_UP。</li>
 * </ol>
 */
public final class CouponCalculator {

    private static final int SCALE = 2;

    private CouponCalculator() {
    }

    /**
     * @param categoryParents 商品行类目 ID → 父类目 ID，用于「券配置一级类目，商品挂在二级类目」的匹配；可为空
     */
    public static Result evaluate(Coupon coupon, List<GoodsLine> lines, Map<Long, Long> categoryParents) {
        BigDecimal applicable = applicableAmount(coupon, lines, categoryParents);
        if (applicable.signum() <= 0) {
            return Result.unusable(applicable, "无适用商品");
        }
        CouponType type = CouponType.valueOf(coupon.getType());
        BigDecimal threshold = nz(coupon.getThreshold());
        if (type != CouponType.NO_THRESHOLD && threshold.signum() > 0 && applicable.compareTo(threshold) < 0) {
            return Result.unusable(applicable, "适用商品金额未满 " + threshold.setScale(SCALE, RoundingMode.HALF_UP) + " 元");
        }
        BigDecimal off = switch (type) {
            case FULL_REDUCTION, NO_THRESHOLD -> nz(coupon.getAmount());
            case DISCOUNT -> discountOff(applicable, coupon.getDiscount(), coupon.getMaxAmount());
        };
        off = off.min(applicable).setScale(SCALE, RoundingMode.HALF_UP);
        if (off.signum() <= 0) {
            return Result.unusable(applicable, "优惠金额为 0");
        }
        return new Result(true, applicable, off, null);
    }

    /** 折扣优惠 = 适用金额 × (1 - 折扣率)，先四舍五入到分再封顶，保证与页面展示一致 */
    static BigDecimal discountOff(BigDecimal applicable, BigDecimal discount, BigDecimal maxAmount) {
        BigDecimal rate = discount == null ? BigDecimal.ONE : discount;
        BigDecimal off = applicable.multiply(BigDecimal.ONE.subtract(rate)).setScale(SCALE, RoundingMode.HALF_UP);
        if (off.signum() < 0) {
            off = BigDecimal.ZERO;
        }
        if (maxAmount != null && maxAmount.signum() > 0) {
            off = off.min(maxAmount);
        }
        return off;
    }

    /** 适用金额：按范围筛选商品行求和 */
    public static BigDecimal applicableAmount(Coupon coupon, List<GoodsLine> lines, Map<Long, Long> categoryParents) {
        BigDecimal sum = BigDecimal.ZERO;
        if (lines == null) {
            return sum;
        }
        for (GoodsLine line : lines) {
            if (matches(coupon, line, categoryParents)) {
                sum = sum.add(nz(line.amount()));
            }
        }
        return sum;
    }

    static boolean matches(Coupon coupon, GoodsLine line, Map<Long, Long> categoryParents) {
        CouponScope scope = coupon.getScope() == null ? CouponScope.ALL : CouponScope.valueOf(coupon.getScope());
        List<Long> ids = coupon.getScopeIds() == null ? List.of() : coupon.getScopeIds();
        return switch (scope) {
            case ALL -> true;
            case GOODS -> line.goodsId() != null && ids.contains(line.goodsId());
            case CATEGORY -> {
                Long cid = line.categoryId();
                if (cid == null) {
                    yield false;
                }
                Long parent = categoryParents == null ? null : categoryParents.get(cid);
                yield ids.contains(cid) || (parent != null && ids.contains(parent));
            }
        };
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 计算结果。
     *
     * @param usable           是否满足使用条件（不含时间、状态校验，由调用方负责）
     * @param applicableAmount 适用商品金额
     * @param discountAmount   优惠金额，不可用时为 0
     * @param reason           不可用原因
     */
    public record Result(boolean usable, BigDecimal applicableAmount, BigDecimal discountAmount, String reason) {

        static Result unusable(BigDecimal applicable, String reason) {
            return new Result(false, applicable, BigDecimal.ZERO.setScale(SCALE), reason);
        }
    }
}
