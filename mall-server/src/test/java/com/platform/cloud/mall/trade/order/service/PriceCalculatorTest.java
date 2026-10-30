package com.platform.cloud.mall.trade.order.service;

import com.platform.cloud.mall.goods.api.GoodsApi.GoodsLine;
import com.platform.cloud.mall.trade.order.service.PriceCalculator.FreightRule;
import com.platform.cloud.mall.trade.order.service.PriceCalculator.Line;
import com.platform.cloud.mall.trade.order.service.PriceCalculator.LineResult;
import com.platform.cloud.mall.trade.order.service.PriceCalculator.Result;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceCalculatorTest {

    private static final FreightRule RULE = new FreightRule(bd("10.00"), bd("99.00"));
    private static final BigDecimal NO_DISCOUNT = BigDecimal.ONE;

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    private static Line line(String price, int qty) {
        return new Line(1L, 11L, 100L, bd(price), qty, false);
    }

    private static Line line(long skuId, String price, int qty) {
        return new Line(skuId * 10, skuId, 100L, bd(price), qty, false);
    }

    private static Line seckill(String price, int qty) {
        return new Line(2L, 22L, 100L, bd(price), qty, true);
    }

    private static BigDecimal sumPay(Result r) {
        return r.lines().stream().map(LineResult::payAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Nested
    class 会员折扣 {

        @Test
        void 无折扣时金额为单价乘数量() {
            Result r = PriceCalculator.calculate(List.of(line("49.90", 3)), NO_DISCOUNT, null, RULE);
            assertThat(r.goodsAmount()).isEqualByComparingTo("149.70");
            assertThat(r.discountAmount()).isEqualByComparingTo("0.00");
            assertThat(r.freightAmount()).isEqualByComparingTo("0.00");
            assertThat(r.payAmount()).isEqualByComparingTo("149.70");
        }

        @Test
        void 九五折逐行四舍五入() {
            // 33.33 × 0.95 = 31.6635 → 31.66，折扣 1.67
            Result r = PriceCalculator.calculate(List.of(line("33.33", 1)), bd("0.95"), null, RULE);
            assertThat(r.discountAmount()).isEqualByComparingTo("1.67");
            assertThat(r.lines().get(0).payAmount()).isEqualByComparingTo("31.66");
            assertThat(r.payAmount()).isEqualByComparingTo("41.66");
        }

        @Test
        void 秒杀行不参与会员折扣() {
            Result r = PriceCalculator.calculate(List.of(seckill("9.90", 2)), bd("0.80"), null, RULE);
            assertThat(r.discountAmount()).isEqualByComparingTo("0.00");
            assertThat(r.payAmount()).isEqualByComparingTo("29.80");
        }

        @Test
        void 混合行只有普通行打折() {
            Result r = PriceCalculator.calculate(List.of(line("100.00", 1), seckill("50.00", 1)), bd("0.90"),
                    null, RULE);
            assertThat(r.discountAmount()).isEqualByComparingTo("10.00");
            assertThat(r.lines().get(0).discountAmount()).isEqualByComparingTo("10.00");
            assertThat(r.lines().get(1).discountAmount()).isEqualByComparingTo("0.00");
            assertThat(r.payAmount()).isEqualByComparingTo("140.00");
        }

        @ParameterizedTest
        @CsvSource({"0", "-0.5", "1.20"})
        void 非法折扣率按不打折处理(String rate) {
            Result r = PriceCalculator.calculate(List.of(line("100.00", 1)), bd(rate), null, RULE);
            assertThat(r.discountAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        void null折扣按不打折处理() {
            Result r = PriceCalculator.calculate(List.of(line("100.00", 1)), null, null, RULE);
            assertThat(r.discountAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        void 券的商品行基于折后金额() {
            List<GoodsLine> lines = PriceCalculator.toGoodsLines(
                    List.of(line(1, "100.00", 2), seckill("30.00", 1)), bd("0.90"));
            assertThat(lines.get(0).amount()).isEqualByComparingTo("180.00");
            assertThat(lines.get(1).amount()).isEqualByComparingTo("30.00");
            assertThat(lines.get(0).categoryId()).isEqualTo(100L);
        }
    }

    @Nested
    class 优惠券 {

        @Test
        void 券金额从折后金额中扣减() {
            Result r = PriceCalculator.calculate(List.of(line("200.00", 1)), bd("0.90"), bd("20.00"), RULE);
            // 200 → 折后 180 → 券 20 → 160，包邮
            assertThat(r.couponAmount()).isEqualByComparingTo("20.00");
            assertThat(r.payAmount()).isEqualByComparingTo("160.00");
        }

        @Test
        void 券金额封顶到折后合计且应付最低0点01() {
            Result r = PriceCalculator.calculate(List.of(line("100.00", 1)), NO_DISCOUNT, bd("500.00"), RULE);
            assertThat(r.couponAmount()).isEqualByComparingTo("100.00");
            assertThat(r.payAmount()).isEqualByComparingTo("0.01");
            // 0.01 兜底补到最后一行，明细合计 = 应付 − 运费
            assertThat(sumPay(r)).isEqualByComparingTo("0.01");
        }

        @Test
        void 负数券金额视为0() {
            Result r = PriceCalculator.calculate(List.of(line("100.00", 1)), NO_DISCOUNT, bd("-5"), RULE);
            assertThat(r.couponAmount()).isEqualByComparingTo("0.00");
            assertThat(r.payAmount()).isEqualByComparingTo("100.00");
        }

        @Test
        void 券刚好抵扣商品但有运费时应付等于运费() {
            Result r = PriceCalculator.calculate(List.of(line("50.00", 1)), NO_DISCOUNT, bd("50.00"), RULE);
            assertThat(r.freightAmount()).isEqualByComparingTo("10.00");
            assertThat(r.payAmount()).isEqualByComparingTo("10.00");
            assertThat(sumPay(r)).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    class 运费 {

        @ParameterizedTest(name = "折后 {0} → 运费 {1}")
        @CsvSource({"99.00, 0.00", "98.99, 10.00", "99.01, 0.00", "0.01, 10.00"})
        void 包邮门槛边界(String amount, String freight) {
            Result r = PriceCalculator.calculate(List.of(line(amount, 1)), NO_DISCOUNT, null, RULE);
            assertThat(r.freightAmount()).isEqualByComparingTo(freight);
        }

        @Test
        void 按会员折扣后金额判断包邮() {
            // 104 × 0.95 = 98.80 < 99，需要运费
            Result r = PriceCalculator.calculate(List.of(line("52.00", 2)), bd("0.95"), null, RULE);
            assertThat(r.freightAmount()).isEqualByComparingTo("10.00");
            assertThat(r.payAmount()).isEqualByComparingTo("108.80");
        }

        @Test
        void 用券后低于门槛仍包邮() {
            // 折后 120 ≥ 99 包邮，用券 30 后 90 也不再收运费
            Result r = PriceCalculator.calculate(List.of(line("120.00", 1)), NO_DISCOUNT, bd("30.00"), RULE);
            assertThat(r.freightAmount()).isEqualByComparingTo("0.00");
            assertThat(r.payAmount()).isEqualByComparingTo("90.00");
        }

        @Test
        void 门槛为0一律包邮() {
            Result r = PriceCalculator.calculate(List.of(line("1.00", 1)), NO_DISCOUNT, null,
                    new FreightRule(bd("10"), BigDecimal.ZERO));
            assertThat(r.freightAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        void 运费为0不收运费() {
            Result r = PriceCalculator.calculate(List.of(line("1.00", 1)), NO_DISCOUNT, null,
                    new FreightRule(BigDecimal.ZERO, bd("99")));
            assertThat(r.freightAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        void 运费不分摊到明细() {
            Result r = PriceCalculator.calculate(List.of(line("30.00", 1)), NO_DISCOUNT, null, RULE);
            assertThat(r.payAmount()).isEqualByComparingTo("40.00");
            assertThat(r.lines().get(0).payAmount()).isEqualByComparingTo("30.00");
        }
    }

    @Nested
    class 分摊 {

        @Test
        void 券按折后金额比例分摊() {
            Result r = PriceCalculator.calculate(List.of(line(1, "100.00", 1), line(2, "200.00", 1)),
                    NO_DISCOUNT, bd("10.00"), RULE);
            assertThat(r.lines().get(0).payAmount()).isEqualByComparingTo("96.67");
            assertThat(r.lines().get(1).payAmount()).isEqualByComparingTo("193.33");
            assertThat(sumPay(r)).isEqualByComparingTo("290.00");
        }

        @Test
        void 尾差计入最后一行() {
            Result r = PriceCalculator.calculate(
                    List.of(line(1, "10.00", 1), line(2, "10.00", 1), line(3, "10.00", 1)),
                    NO_DISCOUNT, bd("10.00"), RULE);
            // 10 / 3 = 3.33, 3.33, 尾差 3.34 → 实付 6.67, 6.67, 6.66
            assertThat(r.lines()).extracting(LineResult::payAmount)
                    .usingElementComparator(BigDecimal::compareTo)
                    .containsExactly(bd("6.67"), bd("6.67"), bd("6.66"));
            assertThat(sumPay(r).add(r.freightAmount())).isEqualByComparingTo(r.payAmount());
        }

        @Test
        void 折扣加券后明细合计等于应付减运费() {
            Result r = PriceCalculator.calculate(
                    List.of(line(1, "19.90", 3), line(2, "88.80", 1), line(3, "5.55", 7)),
                    bd("0.88"), bd("23.45"), RULE);
            assertThat(sumPay(r)).isEqualByComparingTo(r.payAmount().subtract(r.freightAmount()));
            assertThat(r.lines()).allSatisfy(l -> assertThat(l.payAmount()).isNotNegative());
        }

        @Test
        void allocate总额为0时全为0() {
            List<BigDecimal> s = PriceCalculator.allocate(BigDecimal.ZERO, List.of(bd("1"), bd("2")), true);
            assertThat(s).allSatisfy(v -> assertThat(v).isEqualByComparingTo("0"));
        }

        @Test
        void allocate权重全为0时全部计入最后一行() {
            List<BigDecimal> s = PriceCalculator.allocate(bd("0.01"), List.of(bd("0"), bd("0")), false);
            assertThat(s.get(0)).isEqualByComparingTo("0");
            assertThat(s.get(1)).isEqualByComparingTo("0.01");
        }

        @Test
        void allocate四舍五入导致末行为负时从前面行回收() {
            // 0.02 按 1:1:1:0.01 分：前三行各 0.01（向上取整）= 0.03 > 0.02
            List<BigDecimal> s = PriceCalculator.allocate(bd("0.02"),
                    List.of(bd("1"), bd("1"), bd("1"), bd("0.01")), true);
            assertThat(s).allSatisfy(v -> assertThat(v).isNotNegative());
            assertThat(s.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("0.02");
        }

        @Test
        void allocate随机数据下每行不越界且总额守恒() {
            Random random = new Random(20261031L);
            for (int round = 0; round < 2000; round++) {
                int n = 1 + random.nextInt(6);
                List<BigDecimal> weights = new ArrayList<>();
                BigDecimal sum = BigDecimal.ZERO;
                for (int i = 0; i < n; i++) {
                    BigDecimal w = BigDecimal.valueOf(1 + random.nextInt(20000), 2);
                    weights.add(w);
                    sum = sum.add(w);
                }
                BigDecimal total = BigDecimal.valueOf(random.nextLong(sum.movePointRight(2).longValue() + 1), 2);
                List<BigDecimal> shares = PriceCalculator.allocate(total, weights, true);
                assertThat(shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(total);
                for (int i = 0; i < n; i++) {
                    assertThat(shares.get(i)).isNotNegative().isLessThanOrEqualTo(weights.get(i));
                }
            }
        }
    }

    @Test
    void 空行抛异常() {
        assertThatThrownBy(() -> PriceCalculator.calculate(List.of(), NO_DISCOUNT, null, RULE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
