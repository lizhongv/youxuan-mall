package com.platform.cloud.mall.marketing.support;

import com.platform.cloud.mall.goods.api.GoodsApi.GoodsLine;
import com.platform.cloud.mall.marketing.entity.Coupon;
import com.platform.cloud.mall.marketing.support.CouponCalculator.Result;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 优惠券金额计算：三种类型 × 三种范围 × 门槛边界 × 封顶 × 不超过适用金额 × 两位小数。
 */
class CouponCalculatorTest {

    /** 类目：一级 100 下有二级 101、102；一级 200 下有二级 201 */
    private static final Map<Long, Long> PARENTS = Map.of(101L, 100L, 102L, 100L, 201L, 200L);

    private static Coupon fullReduction(String threshold, String amount) {
        Coupon c = new Coupon();
        c.setType("FULL_REDUCTION");
        c.setThreshold(new BigDecimal(threshold));
        c.setAmount(new BigDecimal(amount));
        c.setScope("ALL");
        return c;
    }

    private static Coupon noThreshold(String amount) {
        Coupon c = new Coupon();
        c.setType("NO_THRESHOLD");
        c.setThreshold(BigDecimal.ZERO);
        c.setAmount(new BigDecimal(amount));
        c.setScope("ALL");
        return c;
    }

    private static Coupon discount(String rate, String max, String threshold) {
        Coupon c = new Coupon();
        c.setType("DISCOUNT");
        c.setDiscount(new BigDecimal(rate));
        c.setMaxAmount(new BigDecimal(max));
        c.setThreshold(new BigDecimal(threshold));
        c.setAmount(BigDecimal.ZERO);
        c.setScope("ALL");
        return c;
    }

    private static GoodsLine line(long goodsId, long categoryId, String amount) {
        return new GoodsLine(goodsId, categoryId, new BigDecimal(amount));
    }

    private static Coupon scoped(Coupon c, String scope, Long... ids) {
        c.setScope(scope);
        c.setScopeIds(List.of(ids));
        return c;
    }

    @Nested
    class FullReduction {

        @ParameterizedTest(name = "门槛 99 减 10，金额 {0} → 可用 {1}，优惠 {2}")
        @CsvSource({
                "98.99, false, 0.00",
                "99.00, true, 10.00",
                "99.01, true, 10.00",
                "500.00, true, 10.00"
        })
        void thresholdBoundary(String amount, boolean usable, String off) {
            Result r = CouponCalculator.evaluate(fullReduction("99", "10"), List.of(line(1, 101, amount)), PARENTS);
            assertThat(r.usable()).isEqualTo(usable);
            assertThat(r.discountAmount()).isEqualByComparingTo(off);
            assertThat(r.discountAmount().scale()).isEqualTo(2);
        }

        @Test
        void reasonMentionsThreshold() {
            Result r = CouponCalculator.evaluate(fullReduction("299", "40"), List.of(line(1, 101, "100")), PARENTS);
            assertThat(r.usable()).isFalse();
            assertThat(r.reason()).contains("299.00");
        }

        @Test
        void multipleLinesSummed() {
            Result r = CouponCalculator.evaluate(fullReduction("99", "10"),
                    List.of(line(1, 101, "50"), line(2, 201, "49")), PARENTS);
            assertThat(r.usable()).isTrue();
            assertThat(r.applicableAmount()).isEqualByComparingTo("99");
        }

        @Test
        void zeroThresholdAlwaysReaches() {
            Result r = CouponCalculator.evaluate(fullReduction("0", "3"), List.of(line(1, 101, "1")), PARENTS);
            assertThat(r.usable()).isTrue();
            assertThat(r.discountAmount()).isEqualByComparingTo("1.00");
        }
    }

    @Nested
    class NoThreshold {

        @Test
        void directReduction() {
            Result r = CouponCalculator.evaluate(noThreshold("5"), List.of(line(1, 101, "30")), PARENTS);
            assertThat(r.usable()).isTrue();
            assertThat(r.discountAmount()).isEqualByComparingTo("5.00");
        }

        @Test
        void notExceedApplicableAmount() {
            Result r = CouponCalculator.evaluate(noThreshold("5"), List.of(line(1, 101, "3.20")), PARENTS);
            assertThat(r.usable()).isTrue();
            assertThat(r.discountAmount()).isEqualByComparingTo("3.20");
        }

        @Test
        void thresholdIgnoredEvenIfDirty() {
            Coupon c = noThreshold("5");
            c.setThreshold(new BigDecimal("1000"));
            Result r = CouponCalculator.evaluate(c, List.of(line(1, 101, "10")), PARENTS);
            assertThat(r.usable()).isTrue();
            assertThat(r.discountAmount()).isEqualByComparingTo("5.00");
        }
    }

    @Nested
    class Discount {

        @ParameterizedTest(name = "折扣 {0} 封顶 {1}，金额 {2} → 优惠 {3}")
        @CsvSource({
                // 9 折最高减 50：100 元减 10；500 元正好 50；600 元封顶 50
                "0.90, 50, 100.00, 10.00",
                "0.90, 50, 500.00, 50.00",
                "0.90, 50, 600.00, 50.00",
                // 不封顶
                "0.90, 0, 600.00, 60.00",
                "0.80, 0, 99.99, 20.00",
                // 四舍五入到分：33.33 × 0.15 = 4.9995 → 5.00
                "0.85, 0, 33.33, 5.00",
                // 12.34 × 0.1 = 1.234 → 1.23
                "0.90, 0, 12.34, 1.23",
                // 1.25 × 0.1 = 0.125 → 0.13（HALF_UP）
                "0.90, 0, 1.25, 0.13"
        })
        void amounts(String rate, String max, String amount, String off) {
            Result r = CouponCalculator.evaluate(discount(rate, max, "0"), List.of(line(1, 101, amount)), PARENTS);
            assertThat(r.usable()).isTrue();
            assertThat(r.discountAmount()).isEqualByComparingTo(off);
            assertThat(r.discountAmount().scale()).isEqualTo(2);
        }

        @Test
        void thresholdApplies() {
            Coupon c = discount("0.90", "0", "100");
            assertThat(CouponCalculator.evaluate(c, List.of(line(1, 101, "99.99")), PARENTS).usable()).isFalse();
            assertThat(CouponCalculator.evaluate(c, List.of(line(1, 101, "100")), PARENTS).usable()).isTrue();
        }

        @Test
        void tooSmallAmountRoundsToZeroIsUnusable() {
            Result r = CouponCalculator.evaluate(discount("0.90", "0", "0"), List.of(line(1, 101, "0.04")), PARENTS);
            assertThat(r.usable()).isFalse();
            assertThat(r.discountAmount()).isEqualByComparingTo("0");
        }

        @Test
        void discountOnlyOnApplicablePart() {
            Coupon c = scoped(discount("0.80", "0", "0"), "GOODS", 1L);
            Result r = CouponCalculator.evaluate(c, List.of(line(1, 101, "100"), line(2, 101, "900")), PARENTS);
            assertThat(r.applicableAmount()).isEqualByComparingTo("100");
            assertThat(r.discountAmount()).isEqualByComparingTo("20.00");
        }
    }

    @Nested
    class Scope {

        @Test
        void goodsScopeOnlyCountsListedGoods() {
            Coupon c = scoped(fullReduction("99", "10"), "GOODS", 1L, 3L);
            Result r = CouponCalculator.evaluate(c, List.of(line(1, 101, "60"), line(2, 101, "60")), PARENTS);
            assertThat(r.applicableAmount()).isEqualByComparingTo("60");
            assertThat(r.usable()).isFalse();
        }

        @Test
        void goodsScopeNoMatch() {
            Coupon c = scoped(noThreshold("5"), "GOODS", 9L);
            Result r = CouponCalculator.evaluate(c, List.of(line(1, 101, "60")), PARENTS);
            assertThat(r.usable()).isFalse();
            assertThat(r.reason()).isEqualTo("无适用商品");
        }

        @Test
        void secondLevelCategoryScope() {
            Coupon c = scoped(fullReduction("99", "10"), "CATEGORY", 101L);
            Result r = CouponCalculator.evaluate(c, List.of(line(1, 101, "100"), line(2, 102, "100")), PARENTS);
            assertThat(r.applicableAmount()).isEqualByComparingTo("100");
            assertThat(r.discountAmount()).isEqualByComparingTo("10.00");
        }

        @Test
        void firstLevelCategoryCoversChildren() {
            Coupon c = scoped(fullReduction("99", "10"), "CATEGORY", 100L);
            Result r = CouponCalculator.evaluate(c,
                    List.of(line(1, 101, "50"), line(2, 102, "50"), line(3, 201, "1000")), PARENTS);
            assertThat(r.applicableAmount()).isEqualByComparingTo("100");
            assertThat(r.usable()).isTrue();
        }

        @Test
        void categoryWithoutParentInfoOnlyMatchesDirectly() {
            Coupon c = scoped(noThreshold("5"), "CATEGORY", 100L);
            Result r = CouponCalculator.evaluate(c, List.of(line(1, 101, "50")), Map.of());
            assertThat(r.usable()).isFalse();
        }

        @Test
        void allScope() {
            Result r = CouponCalculator.evaluate(fullReduction("99", "10"),
                    List.of(line(1, 101, "50"), line(2, 201, "50")), PARENTS);
            assertThat(r.applicableAmount()).isEqualByComparingTo("100");
            assertThat(r.usable()).isTrue();
        }

        @Test
        void thresholdJudgedOnApplicableAmountNotOrderTotal() {
            // 订单总额 1000，但适用类目只有 80，不满 99
            Coupon c = scoped(fullReduction("99", "10"), "CATEGORY", 200L);
            Result r = CouponCalculator.evaluate(c, List.of(line(1, 101, "920"), line(2, 201, "80")), PARENTS);
            assertThat(r.usable()).isFalse();
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void emptyLines() {
            assertThat(CouponCalculator.evaluate(noThreshold("5"), List.of(), PARENTS).usable()).isFalse();
            assertThat(CouponCalculator.evaluate(noThreshold("5"), null, PARENTS).usable()).isFalse();
        }

        @Test
        void nullAmountTreatedAsZero() {
            Result r = CouponCalculator.evaluate(noThreshold("5"),
                    List.of(new GoodsLine(1L, 101L, null), line(2, 101, "8")), PARENTS);
            assertThat(r.applicableAmount()).isEqualByComparingTo("8");
        }

        @Test
        void reductionLargerThanApplicableCapped() {
            Result r = CouponCalculator.evaluate(fullReduction("10", "50"), List.of(line(1, 101, "20")), PARENTS);
            assertThat(r.discountAmount()).isEqualByComparingTo("20.00");
        }
    }
}
