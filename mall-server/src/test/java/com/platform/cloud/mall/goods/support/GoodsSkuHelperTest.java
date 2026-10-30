package com.platform.cloud.mall.goods.support;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.goods.dto.GoodsForm.SkuForm;
import com.platform.cloud.mall.goods.entity.SpecItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SKU 校验、规格文本与 SPU 汇总。
 */
class GoodsSkuHelperTest {

    private static final List<SpecItem> COLOR_SIZE = List.of(
            new SpecItem("颜色", List.of("红", "蓝")),
            new SpecItem("尺码", List.of("M", "L")));

    private static SkuForm sku(String price, String original, Integer stock, String... values) {
        SkuForm f = new SkuForm();
        f.setSpecValues(new ArrayList<>(List.of(values)));
        f.setPrice(price == null ? null : new BigDecimal(price));
        f.setOriginalPrice(original == null ? null : new BigDecimal(original));
        f.setStock(stock);
        return f;
    }

    @Test
    void validMultiSpec() {
        assertThatCode(() -> GoodsSkuHelper.validate(COLOR_SIZE, List.of(
                sku("99", "129", 10, "红", "M"),
                sku("109", "139", 0, "蓝", "L")))).doesNotThrowAnyException();
    }

    @Test
    void validNoSpec() {
        assertThatCode(() -> GoodsSkuHelper.validate(List.of(), List.of(sku("9.9", null, 5))))
                .doesNotThrowAnyException();
        assertThatCode(() -> GoodsSkuHelper.validate(null, List.of(sku("9.9", null, 5))))
                .doesNotThrowAnyException();
    }

    @Test
    void noSpecMustHaveExactlyOneSku() {
        assertThatThrownBy(() -> GoodsSkuHelper.validate(List.of(), List.of(sku("1", null, 1), sku("2", null, 1))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("无规格商品只能有一个 SKU");
    }

    @Test
    void emptySkus() {
        assertThatThrownBy(() -> GoodsSkuHelper.validate(COLOR_SIZE, List.of()))
                .hasMessageContaining("至少需要一个 SKU");
    }

    @Test
    void specValuesLengthMismatch() {
        assertThatThrownBy(() -> GoodsSkuHelper.validate(COLOR_SIZE, List.of(sku("1", null, 1, "红"))))
                .hasMessageContaining("规格值与规格项不一致");
        assertThatThrownBy(() -> GoodsSkuHelper.validate(List.of(), List.of(sku("1", null, 1, "红"))))
                .hasMessageContaining("规格值与规格项不一致");
    }

    @Test
    void specValueNotDefined() {
        assertThatThrownBy(() -> GoodsSkuHelper.validate(COLOR_SIZE, List.of(sku("1", null, 1, "绿", "M"))))
                .hasMessageContaining("绿");
        // 顺序错位也算不一致
        assertThatThrownBy(() -> GoodsSkuHelper.validate(COLOR_SIZE, List.of(sku("1", null, 1, "M", "红"))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void duplicateCombination() {
        assertThatThrownBy(() -> GoodsSkuHelper.validate(COLOR_SIZE, List.of(
                sku("1", null, 1, "红", "M"), sku("2", null, 1, "红", "M"))))
                .hasMessageContaining("组合重复");
    }

    @Test
    void priceMustBePositive() {
        assertThatThrownBy(() -> GoodsSkuHelper.validate(COLOR_SIZE, List.of(sku("0", null, 1, "红", "M"))))
                .hasMessageContaining("价格必须大于 0");
        assertThatThrownBy(() -> GoodsSkuHelper.validate(COLOR_SIZE, List.of(sku("-1", null, 1, "红", "M"))))
                .hasMessageContaining("价格必须大于 0");
        assertThatThrownBy(() -> GoodsSkuHelper.validate(COLOR_SIZE, List.of(sku(null, null, 1, "红", "M"))))
                .hasMessageContaining("价格必须大于 0");
    }

    @Test
    void stockMustBeNonNegative() {
        assertThatThrownBy(() -> GoodsSkuHelper.validate(COLOR_SIZE, List.of(sku("1", null, -1, "红", "M"))))
                .hasMessageContaining("库存不能小于 0");
        assertThatThrownBy(() -> GoodsSkuHelper.validate(COLOR_SIZE, List.of(sku("1", null, null, "红", "M"))))
                .hasMessageContaining("库存不能小于 0");
    }

    @Test
    void negativeOriginalPrice() {
        assertThatThrownBy(() -> GoodsSkuHelper.validate(COLOR_SIZE, List.of(sku("1", "-2", 1, "红", "M"))))
                .hasMessageContaining("原价");
    }

    @Test
    void specItemDefinitionErrors() {
        assertThatThrownBy(() -> GoodsSkuHelper.validate(List.of(new SpecItem("颜色", List.of("红")),
                new SpecItem("颜色", List.of("蓝"))), List.of(sku("1", null, 1, "红", "蓝"))))
                .hasMessageContaining("规格名重复");
        assertThatThrownBy(() -> GoodsSkuHelper.validate(List.of(new SpecItem("颜色", List.of("红", "红"))),
                List.of(sku("1", null, 1, "红"))))
                .hasMessageContaining("值重复");
        assertThatThrownBy(() -> GoodsSkuHelper.validate(List.of(new SpecItem("颜色", List.of())),
                List.of(sku("1", null, 1, "红"))))
                .hasMessageContaining("至少需要一个值");
        assertThatThrownBy(() -> GoodsSkuHelper.validate(List.of(new SpecItem(" ", List.of("红"))),
                List.of(sku("1", null, 1, "红"))))
                .hasMessageContaining("规格名不能为空");
    }

    @Test
    void tooManySpecItems() {
        List<SpecItem> items = List.of(new SpecItem("a", List.of("1")), new SpecItem("b", List.of("1")),
                new SpecItem("c", List.of("1")), new SpecItem("d", List.of("1")));
        assertThatThrownBy(() -> GoodsSkuHelper.validate(items, List.of(sku("1", null, 1, "1", "1", "1", "1"))))
                .hasMessageContaining("规格最多");
    }

    @Test
    void normalizeTrims() {
        List<SpecItem> n = GoodsSkuHelper.normalize(List.of(new SpecItem(" 颜色 ", List.of(" 红 "))));
        assertThat(n.get(0).getName()).isEqualTo("颜色");
        assertThat(n.get(0).getValues()).containsExactly("红");
        assertThat(GoodsSkuHelper.normalize(null)).isEmpty();
    }

    @Test
    void specText() {
        assertThat(GoodsSkuHelper.specText(COLOR_SIZE, List.of("红", "L"))).isEqualTo("颜色:红;尺码:L");
        assertThat(GoodsSkuHelper.specText(List.of(), List.of())).isEmpty();
    }

    @Test
    void summarizeTakesMinPriceItsOriginalAndSumStock() {
        GoodsSkuHelper.Summary s = GoodsSkuHelper.summarize(List.of(
                sku("109", "139", 5, "蓝", "L"),
                sku("99", "129", 10, "红", "M"),
                sku("199", "259", 7, "红", "L")));
        assertThat(s.price()).isEqualByComparingTo("99.00");
        assertThat(s.price().scale()).isEqualTo(2);
        assertThat(s.originalPrice()).isEqualByComparingTo("129.00");
        assertThat(s.stock()).isEqualTo(22);
    }

    @Test
    void summarizeTieUsesHigherOriginal() {
        GoodsSkuHelper.Summary s = GoodsSkuHelper.summarize(List.of(
                sku("99", "109", 1, "红", "M"),
                sku("99", "129", 1, "蓝", "M")));
        assertThat(s.originalPrice()).isEqualByComparingTo("129");
    }

    @Test
    void summarizeNullOriginalBecomesZero() {
        GoodsSkuHelper.Summary s = GoodsSkuHelper.summarize(List.of(sku("9.999", null, 3)));
        assertThat(s.price()).isEqualByComparingTo("10.00");
        assertThat(s.originalPrice()).isEqualByComparingTo("0");
        assertThat(s.stock()).isEqualTo(3);
    }
}
