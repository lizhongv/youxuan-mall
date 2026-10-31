package com.platform.cloud.mall.trade.freight.support;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.trade.freight.support.FreightCalculator.ChargeType;
import com.platform.cloud.mall.trade.freight.support.FreightCalculator.Free;
import com.platform.cloud.mall.trade.freight.support.FreightCalculator.Item;
import com.platform.cloud.mall.trade.freight.support.FreightCalculator.Rule;
import com.platform.cloud.mall.trade.freight.support.FreightCalculator.Template;
import com.platform.cloud.mall.trade.order.service.PriceCalculator.FreightRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 运费模板计算：按件 / 按重、首续、地区匹配、指定包邮、多模板合并、无模板回退、全局满额包邮优先。
 */
class FreightCalculatorTest {

    private static final Long T_COUNT = 1L;
    private static final Long T_WEIGHT = 2L;
    /** 全局：一口价 10，满 99 包邮 */
    private static final FreightRule GLOBAL = new FreightRule(new BigDecimal("10"), new BigDecimal("99"));
    /** 全局门槛关闭（0）：只对无模板商品表示一律包邮 */
    private static final FreightRule GLOBAL_NO_THRESHOLD = new FreightRule(new BigDecimal("10"), BigDecimal.ZERO);

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    /** 按件：默认地区首件 1 件 8 元，续 1 件 2 元；新疆西藏首件 20 元续 1 件 10 元 */
    private static Template byCount(List<Free> frees) {
        return new Template(T_COUNT, "按件模板", ChargeType.BY_COUNT, List.of(
                new Rule(List.of("新疆维吾尔自治区", "西藏自治区"), d("1"), d("20"), d("1"), d("10")),
                new Rule(List.of(), d("1"), d("8"), d("1"), d("2"))), frees);
    }

    /** 按重：默认地区首重 1kg 6 元，续重 0.5kg 1.5 元 */
    private static Template byWeight() {
        return new Template(T_WEIGHT, "按重模板", ChargeType.BY_WEIGHT, List.of(
                new Rule(List.of(), d("1"), d("6"), d("0.5"), d("1.5"))), List.of());
    }

    private static Item item(Long tid, int qty, String weight, String amount) {
        return new Item(tid, qty, weight == null ? null : d(weight), d(amount));
    }

    @Test
    void 按件_首件内只收首费() {
        BigDecimal f = FreightCalculator.calculate("广东省", List.of(item(T_COUNT, 1, null, "20")),
                Map.of(T_COUNT, byCount(List.of())), null, GLOBAL);
        assertThat(f).isEqualByComparingTo("8.00");
    }

    @Test
    void 按件_同模板多商品合并计件_首续() {
        // 2 + 1 = 3 件：首件 8 + 续 2 件 × 2 = 12
        BigDecimal f = FreightCalculator.calculate("广东省",
                List.of(item(T_COUNT, 2, null, "20"), item(T_COUNT, 1, null, "10")),
                Map.of(T_COUNT, byCount(List.of())), null, GLOBAL);
        assertThat(f).isEqualByComparingTo("12.00");
    }

    @Test
    void 指定地区规则优先于默认地区_省名写法不同也能匹配() {
        // 「新疆」匹配「新疆维吾尔自治区」：首件 20 + 续 1 件 10
        BigDecimal f = FreightCalculator.calculate("新疆", List.of(item(T_COUNT, 2, null, "20")),
                Map.of(T_COUNT, byCount(List.of())), null, GLOBAL);
        assertThat(f).isEqualByComparingTo("30.00");
    }

    @Test
    void 按重_续重向上取整() {
        // 3 件 × 0.6kg = 1.8kg：首重 1kg 6 元，超出 0.8kg → 2 档 × 1.5 = 3，合计 9
        BigDecimal f = FreightCalculator.calculate("浙江省", List.of(item(T_WEIGHT, 3, "0.6", "30")),
                Map.of(T_WEIGHT, byWeight()), null, GLOBAL);
        assertThat(f).isEqualByComparingTo("9.00");
    }

    @Test
    void 按重_未填重量按首重收费() {
        BigDecimal f = FreightCalculator.calculate("浙江省", List.of(item(T_WEIGHT, 5, null, "30")),
                Map.of(T_WEIGHT, byWeight()), null, GLOBAL);
        assertThat(f).isEqualByComparingTo("6.00");
    }

    @Test
    void 续件为0表示不再续费() {
        Rule r = new Rule(List.of(), d("1"), d("5"), BigDecimal.ZERO, d("3"));
        assertThat(FreightCalculator.fee(r, d("10"))).isEqualByComparingTo("5.00");
    }

    @Test
    void 指定地区满件包邮() {
        Template t = byCount(List.of(new Free(List.of("广东省"), 3, BigDecimal.ZERO)));
        Map<Long, Template> ts = Map.of(T_COUNT, t);
        assertThat(FreightCalculator.calculate("广东", List.of(item(T_COUNT, 3, null, "30")), ts, null, GLOBAL))
                .isEqualByComparingTo("0.00");
        // 件数不够
        assertThat(FreightCalculator.calculate("广东", List.of(item(T_COUNT, 2, null, "30")), ts, null, GLOBAL))
                .isEqualByComparingTo("10.00");
        // 地区不命中
        assertThat(FreightCalculator.calculate("浙江省", List.of(item(T_COUNT, 3, null, "30")), ts, null, GLOBAL))
                .isEqualByComparingTo("12.00");
    }

    @Test
    void 全部地区满额包邮_按会员折扣后金额() {
        Template t = byCount(List.of(new Free(List.of(), 0, d("50"))));
        Map<Long, Template> ts = Map.of(T_COUNT, t);
        assertThat(FreightCalculator.calculate("西藏", List.of(item(T_COUNT, 1, null, "50")), ts, null, GLOBAL))
                .isEqualByComparingTo("0.00");
        assertThat(FreightCalculator.calculate("西藏", List.of(item(T_COUNT, 1, null, "49.99")), ts, null, GLOBAL))
                .isEqualByComparingTo("20.00");
    }

    @Test
    void 多模板分别计算后相加() {
        BigDecimal f = FreightCalculator.calculate("广东省",
                List.of(item(T_COUNT, 2, null, "20"), item(T_WEIGHT, 1, "1.2", "20")),
                Map.of(T_COUNT, byCount(List.of()), T_WEIGHT, byWeight()), null, GLOBAL);
        // 按件 8 + 2 = 10；按重 1.2kg → 6 + 1.5 = 7.5；合计 17.5
        assertThat(f).isEqualByComparingTo("17.50");
    }

    @Test
    void 未指定模板的商品使用默认模板_与同模板商品合并计件() {
        BigDecimal f = FreightCalculator.calculate("广东省",
                List.of(item(null, 1, null, "10"), item(T_COUNT, 1, null, "10")),
                Map.of(T_COUNT, byCount(List.of())), T_COUNT, GLOBAL);
        assertThat(f).isEqualByComparingTo("10.00");
    }

    @Test
    void 指定模板已停用或不存在时回退默认模板() {
        BigDecimal f = FreightCalculator.calculate("广东省", List.of(item(99L, 1, "2", "10")),
                Map.of(T_WEIGHT, byWeight()), T_WEIGHT, GLOBAL);
        // 2kg：6 + 2 档 × 1.5 = 9
        assertThat(f).isEqualByComparingTo("9.00");
    }

    @Test
    void 无模板时沿用全局一口价与满额包邮() {
        assertThat(FreightCalculator.calculate("广东省", List.of(item(null, 1, null, "50")), Map.of(), null, GLOBAL))
                .isEqualByComparingTo("10.00");
        assertThat(FreightCalculator.calculate("广东省", List.of(item(null, 1, null, "99")), Map.of(), null, GLOBAL))
                .isEqualByComparingTo("0.00");
        // 门槛为 0 在旧规则里是一律包邮
        assertThat(FreightCalculator.calculate("广东省", List.of(item(null, 1, null, "1")), Map.of(), null,
                GLOBAL_NO_THRESHOLD)).isEqualByComparingTo("0.00");
    }

    @Test
    void 模板商品与无模板商品混合_运费相加() {
        BigDecimal f = FreightCalculator.calculate("广东省",
                List.of(item(T_COUNT, 1, null, "20"), item(null, 1, null, "20")),
                Map.of(T_COUNT, byCount(List.of())), null, GLOBAL);
        assertThat(f).isEqualByComparingTo("18.00");
    }

    @Test
    void 全局满额包邮优先于模板() {
        BigDecimal f = FreightCalculator.calculate("新疆", List.of(item(T_COUNT, 5, null, "120")),
                Map.of(T_COUNT, byCount(List.of())), null, GLOBAL);
        assertThat(f).isEqualByComparingTo("0.00");
    }

    @Test
    void 全局门槛为0不会免掉模板运费() {
        BigDecimal f = FreightCalculator.calculate("广东省", List.of(item(T_COUNT, 1, null, "500")),
                Map.of(T_COUNT, byCount(List.of())), null, GLOBAL_NO_THRESHOLD);
        assertThat(f).isEqualByComparingTo("8.00");
    }

    @Test
    void 地址不在配送范围_提交时报错_预览时按0估算() {
        Template onlyGd = new Template(T_COUNT, "仅广东", ChargeType.BY_COUNT,
                List.of(new Rule(List.of("广东省"), d("1"), d("5"), d("1"), d("1"))), List.of());
        Map<Long, Template> ts = Map.of(T_COUNT, onlyGd);
        assertThatThrownBy(() -> FreightCalculator.calculate("浙江省", List.of(item(T_COUNT, 1, null, "10")), ts,
                null, GLOBAL)).isInstanceOf(BusinessException.class).hasMessageContaining("仅广东");
        assertThat(FreightCalculator.calculate(null, List.of(item(T_COUNT, 1, null, "10")), ts, null, GLOBAL))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void 未选地址时按默认地区规则预估() {
        BigDecimal f = FreightCalculator.calculate(null, List.of(item(T_COUNT, 2, null, "10")),
                Map.of(T_COUNT, byCount(List.of())), null, GLOBAL);
        assertThat(f).isEqualByComparingTo("10.00");
    }

    @Test
    void 省份名规整() {
        assertThat(FreightCalculator.normalize("广西壮族自治区")).isEqualTo("广西");
        assertThat(FreightCalculator.normalize("内蒙古自治区")).isEqualTo("内蒙古");
        assertThat(FreightCalculator.normalize("北京市")).isEqualTo("北京");
        assertThat(FreightCalculator.normalize("香港特别行政区")).isEqualTo("香港");
        assertThat(FreightCalculator.normalize(" 黑龙江省 ")).isEqualTo("黑龙江");
        assertThat(FreightCalculator.normalize("  ")).isNull();
    }

    @Test
    void 空商品运费为0() {
        assertThat(FreightCalculator.calculate("广东省", List.of(), Map.of(), null, GLOBAL)).isEqualByComparingTo("0");
    }
}
