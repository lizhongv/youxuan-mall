package com.platform.cloud.mall.goods.support;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.goods.dto.GoodsForm.SkuForm;
import com.platform.cloud.mall.goods.entity.SpecItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 商品规格与 SKU 的纯计算逻辑：校验、规格文本、SPU 汇总。不依赖 Spring，便于单测覆盖各种边界。
 */
public final class GoodsSkuHelper {

    /** 规格维度上限：主流电商一般 1~3 维，再多前台规格选择器很难用 */
    public static final int MAX_SPEC_ITEMS = 3;
    /** 单商品 SKU 上限：3 维 × 每维十几个值已足够，防止误操作生成上万 SKU */
    public static final int MAX_SKUS = 200;

    private GoodsSkuHelper() {
    }

    /**
     * 规整规格定义：去掉首尾空白，返回新列表，不改入参。
     */
    public static List<SpecItem> normalize(List<SpecItem> specItems) {
        List<SpecItem> result = new ArrayList<>();
        if (specItems == null) {
            return result;
        }
        for (SpecItem item : specItems) {
            List<String> values = new ArrayList<>();
            if (item.getValues() != null) {
                for (String v : item.getValues()) {
                    values.add(v == null ? "" : v.trim());
                }
            }
            result.add(new SpecItem(item.getName() == null ? "" : item.getName().trim(), values));
        }
        return result;
    }

    /**
     * 校验规格定义与 SKU 是否一致。规则：
     * <ul>
     *   <li>规格名非空且不重复；每个规格至少一个值，值非空且不重复；</li>
     *   <li>无规格商品只能有一个 SKU，且其 specValues 为空；</li>
     *   <li>每个 SKU 的 specValues 长度等于规格数，取值必须在对应规格的值里，组合不重复；</li>
     *   <li>价格 &gt; 0，原价 ≥ 0（可空），库存 ≥ 0。</li>
     * </ul>
     * 不强制覆盖全部笛卡尔组合：缺货组合可以不建 SKU，前台选择器会把它置灰。
     */
    public static void validate(List<SpecItem> specItems, List<SkuForm> skus) {
        List<SpecItem> items = specItems == null ? List.of() : specItems;
        if (items.size() > MAX_SPEC_ITEMS) {
            throw new BusinessException("规格最多 " + MAX_SPEC_ITEMS + " 个");
        }
        Set<String> names = new HashSet<>();
        for (SpecItem item : items) {
            if (item.getName() == null || item.getName().isBlank()) {
                throw new BusinessException("规格名不能为空");
            }
            if (!names.add(item.getName())) {
                throw new BusinessException("规格名重复：" + item.getName());
            }
            if (item.getValues() == null || item.getValues().isEmpty()) {
                throw new BusinessException("规格「" + item.getName() + "」至少需要一个值");
            }
            Set<String> values = new HashSet<>();
            for (String v : item.getValues()) {
                if (v == null || v.isBlank()) {
                    throw new BusinessException("规格「" + item.getName() + "」存在空值");
                }
                if (!values.add(v)) {
                    throw new BusinessException("规格「" + item.getName() + "」的值重复：" + v);
                }
            }
        }

        if (skus == null || skus.isEmpty()) {
            throw new BusinessException("至少需要一个 SKU");
        }
        if (skus.size() > MAX_SKUS) {
            throw new BusinessException("SKU 最多 " + MAX_SKUS + " 个");
        }
        if (items.isEmpty() && skus.size() != 1) {
            throw new BusinessException("无规格商品只能有一个 SKU");
        }

        Set<List<String>> combos = new HashSet<>();
        for (SkuForm sku : skus) {
            List<String> sv = sku.getSpecValues() == null ? List.of() : sku.getSpecValues();
            if (sv.size() != items.size()) {
                throw new BusinessException("SKU 规格值与规格项不一致");
            }
            for (int i = 0; i < items.size(); i++) {
                if (!items.get(i).getValues().contains(sv.get(i))) {
                    throw new BusinessException("SKU 规格值「" + sv.get(i) + "」不在规格「"
                            + items.get(i).getName() + "」中");
                }
            }
            if (!combos.add(sv)) {
                throw new BusinessException("SKU 规格组合重复：" + String.join("/", sv));
            }
            if (sku.getPrice() == null || sku.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("SKU 价格必须大于 0");
            }
            if (sku.getOriginalPrice() != null && sku.getOriginalPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("SKU 原价不能小于 0");
            }
            if (sku.getStock() == null || sku.getStock() < 0) {
                throw new BusinessException("SKU 库存不能小于 0");
            }
        }
    }

    /**
     * 规格展示文本「颜色:红;尺码:XL」，作为订单明细快照；无规格返回空串。
     */
    public static String specText(List<SpecItem> specItems, List<String> specValues) {
        if (specItems == null || specValues == null || specItems.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < specItems.size() && i < specValues.size(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(specItems.get(i).getName()).append(':').append(specValues.get(i));
        }
        return sb.toString();
    }

    /**
     * SPU 汇总：价格取最低 SKU 价（列表展示「xx 元起」），原价取该最低价 SKU 的原价（保证划线价与售价对应），
     * 最低价相同时取原价较高者；库存为合计。
     */
    public static Summary summarize(List<SkuForm> skus) {
        SkuForm cheapest = null;
        int stock = 0;
        for (SkuForm sku : skus) {
            stock += sku.getStock() == null ? 0 : sku.getStock();
            if (cheapest == null) {
                cheapest = sku;
                continue;
            }
            int cmp = sku.getPrice().compareTo(cheapest.getPrice());
            boolean higherOriginal = orZero(sku.getOriginalPrice()).compareTo(orZero(cheapest.getOriginalPrice())) > 0;
            if (cmp < 0 || (cmp == 0 && higherOriginal)) {
                cheapest = sku;
            }
        }
        if (cheapest == null) {
            throw new BusinessException("至少需要一个 SKU");
        }
        return new Summary(money(cheapest.getPrice()), money(orZero(cheapest.getOriginalPrice())), stock);
    }

    /** 金额统一两位小数 HALF_UP */
    public static BigDecimal money(BigDecimal v) {
        return orZero(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** SPU 汇总结果 */
    public record Summary(BigDecimal price, BigDecimal originalPrice, int stock) {
    }
}
