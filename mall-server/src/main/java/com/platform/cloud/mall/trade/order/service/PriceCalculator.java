package com.platform.cloud.mall.trade.order.service;

import com.platform.cloud.mall.goods.api.GoodsApi.GoodsLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单价格计算（纯函数，不依赖 Spring 与数据库），结算预览与提交订单共用同一套算法，保证两边金额一致。
 * <p>
 * 计算顺序严格按 api.md §2.6：
 * <ol>
 *   <li>商品金额 = Σ 单价 × 数量</li>
 *   <li>会员折扣：逐行按折扣率计算（秒杀行不参与），得到折后金额</li>
 *   <li>优惠券：券金额由调用方基于折后金额（{@link #toGoodsLines}）向营销领域取得，这里只封顶到折后合计</li>
 *   <li>运费：按折后金额（会员折扣后、用券前）判断是否达到包邮门槛</li>
 *   <li>应付 = 商品 − 折扣 − 券 + 运费，最低 0.01</li>
 * </ol>
 * 优惠券按各行折后金额比例分摊到明细 payAmount，尾差计入最后一行；运费不分摊到明细
 * （部分退款不退运费；未发货整单退完时由售后单独退还运费）。
 * 启用运费模板后运费由 FreightCalculator 预先算好，经 {@link #calculateWithFreight} 传入。
 * 所有金额两位小数、HALF_UP。
 */
public final class PriceCalculator {

    static final BigDecimal MIN_PAY = new BigDecimal("0.01");
    private static final int SCALE = 2;

    private PriceCalculator() {
    }

    /** 计价行；seckill 为 true 时不参与会员折扣 */
    public record Line(Long goodsId, Long skuId, Long categoryId, BigDecimal price, int quantity, boolean seckill) {
    }

    /** 运费规则：freeThreshold ≤ 0 表示一律包邮 */
    public record FreightRule(BigDecimal freightAmount, BigDecimal freeThreshold) {
    }

    public record LineResult(Line line, BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal payAmount) {
    }

    public record Result(BigDecimal goodsAmount, BigDecimal discountAmount, BigDecimal couponAmount,
                         BigDecimal freightAmount, BigDecimal payAmount, List<LineResult> lines) {
    }

    /**
     * 会员折扣后的商品行，供优惠券判断门槛与适用范围。
     * 券的门槛按折后金额判断，避免「折扣 + 满减」叠加后实付低于门槛的情况。
     */
    public static List<GoodsLine> toGoodsLines(List<Line> lines, BigDecimal memberDiscount) {
        BigDecimal rate = normalizeDiscount(memberDiscount);
        List<GoodsLine> result = new ArrayList<>(lines.size());
        for (Line l : lines) {
            result.add(new GoodsLine(l.goodsId(), l.categoryId(), discounted(l, rate)));
        }
        return result;
    }

    /**
     * 计算订单金额。
     *
     * @param memberDiscount 会员折扣率（0.95 表示 95 折），null 或非法值按不打折处理
     * @param couponAmount   优惠券金额（已由营销领域计算），null 表示不用券
     */
    public static Result calculate(List<Line> lines, BigDecimal memberDiscount, BigDecimal couponAmount,
                                   FreightRule freightRule) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("计价行不能为空");
        }
        BigDecimal rate = normalizeDiscount(memberDiscount);
        BigDecimal afterDiscount = BigDecimal.ZERO;
        for (Line l : lines) {
            afterDiscount = afterDiscount.add(discounted(l, rate));
        }
        return calculateWithFreight(lines, memberDiscount, couponAmount, freight(afterDiscount, freightRule));
    }

    /**
     * 与 {@link #calculate} 相同，但运费由调用方预先算好（运费模板按地区、件数 / 重量计费）。
     *
     * @param freightAmount 已计算好的运费，null 视为 0
     */
    public static Result calculateWithFreight(List<Line> lines, BigDecimal memberDiscount, BigDecimal couponAmount,
                                              BigDecimal freightAmount) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("计价行不能为空");
        }
        BigDecimal rate = normalizeDiscount(memberDiscount);

        BigDecimal goodsAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        List<BigDecimal> totals = new ArrayList<>(lines.size());
        List<BigDecimal> discountedList = new ArrayList<>(lines.size());
        for (Line l : lines) {
            BigDecimal total = lineTotal(l);
            BigDecimal discounted = discounted(l, rate);
            totals.add(total);
            discountedList.add(discounted);
            goodsAmount = goodsAmount.add(total);
            discountAmount = discountAmount.add(total.subtract(discounted));
        }
        BigDecimal afterDiscount = goodsAmount.subtract(discountAmount);

        // 券金额封顶到折后合计：营销领域已保证不超过适用商品金额，这里再兜一次，防止应付为负
        BigDecimal coupon = couponAmount == null ? BigDecimal.ZERO : scale(couponAmount.max(BigDecimal.ZERO));
        coupon = coupon.min(afterDiscount);

        BigDecimal freight = scale(freightAmount == null ? BigDecimal.ZERO : freightAmount.max(BigDecimal.ZERO));

        BigDecimal raw = afterDiscount.subtract(coupon).add(freight);
        BigDecimal payAmount = raw.max(MIN_PAY);

        // 优惠券按折后金额比例分摊；商品部分实付 = 应付 − 运费（含 0.01 兜底时补到最后一行）
        List<BigDecimal> couponShares = allocate(coupon, discountedList, true);
        List<LineResult> results = new ArrayList<>(lines.size());
        BigDecimal floorBump = payAmount.subtract(raw);
        for (int i = 0; i < lines.size(); i++) {
            BigDecimal pay = discountedList.get(i).subtract(couponShares.get(i));
            if (i == lines.size() - 1) {
                pay = pay.add(floorBump);
            }
            results.add(new LineResult(lines.get(i), totals.get(i),
                    totals.get(i).subtract(discountedList.get(i)), pay));
        }
        return new Result(scale(goodsAmount), scale(discountAmount), coupon, freight, scale(payAmount), results);
    }

    /**
     * 运费：按会员折扣后金额判断包邮，满门槛（≥）包邮。
     * 用折后而非用券后的金额，是为了用券不会让订单「突然又要付运费」，体验更符合用户预期。
     */
    public static BigDecimal freight(BigDecimal afterDiscount, FreightRule rule) {
        if (rule == null || rule.freightAmount() == null || rule.freightAmount().signum() <= 0) {
            return scale(BigDecimal.ZERO);
        }
        BigDecimal threshold = rule.freeThreshold();
        if (threshold == null || threshold.signum() <= 0 || afterDiscount.compareTo(threshold) >= 0) {
            return scale(BigDecimal.ZERO);
        }
        return scale(rule.freightAmount());
    }

    /**
     * 把 total 按 weights 比例分摊：前 n−1 行四舍五入，尾差计入最后一行。
     * <p>
     * capByWeight 为 true 时（分摊优惠）每行分摊额不超过该行权重、不小于 0：
     * 四舍五入在极端比例下可能让最后一行出现负数或超过本行金额，此时把差额挪到前面有余量的行。
     */
    public static List<BigDecimal> allocate(BigDecimal total, List<BigDecimal> weights, boolean capByWeight) {
        int n = weights.size();
        List<BigDecimal> shares = new ArrayList<>(n);
        BigDecimal sumWeight = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal t = scale(total);
        if (n == 0) {
            return shares;
        }
        if (t.signum() == 0 || sumWeight.signum() == 0) {
            for (int i = 0; i < n - 1; i++) {
                shares.add(scale(BigDecimal.ZERO));
            }
            shares.add(t);
            return shares;
        }
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < n - 1; i++) {
            BigDecimal s = t.multiply(weights.get(i)).divide(sumWeight, SCALE, RoundingMode.HALF_UP);
            shares.add(s);
            allocated = allocated.add(s);
        }
        shares.add(t.subtract(allocated));
        if (capByWeight) {
            rebalance(shares, weights);
        }
        return shares;
    }

    /** 修正最后一行越界：负数从前面行回收，超额向前面行补足 */
    private static void rebalance(List<BigDecimal> shares, List<BigDecimal> weights) {
        int last = shares.size() - 1;
        BigDecimal lastShare = shares.get(last);
        if (lastShare.signum() < 0) {
            BigDecimal deficit = lastShare.negate();
            shares.set(last, scale(BigDecimal.ZERO));
            for (int i = last - 1; i >= 0 && deficit.signum() > 0; i--) {
                BigDecimal take = shares.get(i).min(deficit);
                shares.set(i, shares.get(i).subtract(take));
                deficit = deficit.subtract(take);
            }
        } else if (lastShare.compareTo(weights.get(last)) > 0) {
            BigDecimal excess = lastShare.subtract(weights.get(last));
            shares.set(last, scale(weights.get(last)));
            for (int i = last - 1; i >= 0 && excess.signum() > 0; i--) {
                BigDecimal room = weights.get(i).subtract(shares.get(i)).max(BigDecimal.ZERO);
                BigDecimal give = room.min(excess);
                shares.set(i, shares.get(i).add(give));
                excess = excess.subtract(give);
            }
        }
    }

    private static BigDecimal lineTotal(Line l) {
        return scale(l.price().multiply(BigDecimal.valueOf(l.quantity())));
    }

    private static BigDecimal discounted(Line l, BigDecimal rate) {
        BigDecimal total = lineTotal(l);
        return l.seckill() ? total : scale(total.multiply(rate));
    }

    /** 折扣率只接受 (0, 1]；配置错误（如 0 或 1.2）按不打折处理，宁可少优惠也不能白送或加价 */
    static BigDecimal normalizeDiscount(BigDecimal d) {
        if (d == null || d.signum() <= 0 || d.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return d;
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
