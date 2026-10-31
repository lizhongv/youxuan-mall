package com.platform.cloud.mall.trade.freight.support;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.trade.order.service.PriceCalculator;
import com.platform.cloud.mall.trade.order.service.PriceCalculator.FreightRule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 运费计算（纯函数，不依赖 Spring 与数据库），规则对标 CRMEB / 有赞的运费模板：
 * <ol>
 *   <li>全局「满额包邮」（sys_config 门槛 &gt; 0 且整单会员折扣后金额达到门槛）优先，直接包邮；</li>
 *   <li>商品按运费模板分组：商品指定的模板（存在且启用）→ 默认模板 → 都没有则走全局一口价（兼容旧规则）；</li>
 *   <li>同一模板的商品合并计件 / 计重：先按收货省份匹配计费规则（指定地区优先，其次「默认地区」规则），
 *       首件（首重）收首费，超出部分按续件（续重）向上取整计续费；</li>
 *   <li>模板的「指定条件包邮」：收货省份命中且满足「满 N 件」或「满 X 元」之一，该模板运费为 0；</li>
 *   <li>多个模板的运费相加（主流做法：分模板计算后累加，而不是取最大值）。</li>
 * </ol>
 * 金额判断都用会员折扣后、用券前的金额，与原有「用券不会导致重新收运费」的口径一致。
 */
public final class FreightCalculator {

    private static final int SCALE = 2;
    /** 省份名常见后缀，按长度从长到短去除，使「广西壮族自治区」与「广西」能匹配上 */
    private static final String[] PROVINCE_SUFFIXES = {"特别行政区", "维吾尔自治区", "壮族自治区", "回族自治区", "自治区", "省", "市"};

    private FreightCalculator() {
    }

    /** 计费方式：按件 / 按重量（kg） */
    public enum ChargeType {
        BY_COUNT, BY_WEIGHT
    }

    /** 已启用的运费模板 */
    public record Template(Long id, String name, ChargeType chargeType, List<Rule> rules, List<Free> frees) {
    }

    /**
     * 计费规则。provinces 为空表示「默认地区」（全国其他未单独设置的地区）。
     * nextUnit ≤ 0 表示超出首件 / 首重后不再续费。
     */
    public record Rule(List<String> provinces, BigDecimal firstUnit, BigDecimal firstFee, BigDecimal nextUnit,
                       BigDecimal nextFee) {
    }

    /** 指定条件包邮：provinces 为空表示全部地区；minCount / minAmount ≤ 0 表示不按该条件 */
    public record Free(List<String> provinces, int minCount, BigDecimal minAmount) {
    }

    /**
     * 结算行。
     *
     * @param templateId 商品指定的模板，可空
     * @param unitWeight 单件重量（kg），可空视为 0
     * @param amount     该行会员折扣后金额（用于满额包邮判断）
     */
    public record Item(Long templateId, int quantity, BigDecimal unitWeight, BigDecimal amount) {
    }

    /**
     * 计算整单运费。
     *
     * @param province          收货省份；为空（结算预览未选地址）时只匹配默认地区规则，匹配不到按 0 预估
     * @param templates         已启用的模板（按 ID）
     * @param defaultTemplateId 默认模板 ID，可空
     * @param global            全局一口价与满额包邮门槛（sys_config）
     * @throws BusinessException 收货省份不在某个模板的配送范围内
     */
    public static BigDecimal calculate(String province, List<Item> items, Map<Long, Template> templates,
                                       Long defaultTemplateId, FreightRule global) {
        if (items == null || items.isEmpty()) {
            return zero();
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Item it : items) {
            total = total.add(nz(it.amount()));
        }
        if (globalFree(total, global)) {
            return zero();
        }

        Map<Long, List<Item>> groups = new LinkedHashMap<>();
        boolean hasFallback = false;
        for (Item it : items) {
            Long tid = resolveTemplate(it.templateId(), templates, defaultTemplateId);
            if (tid == null) {
                hasFallback = true;
            } else {
                groups.computeIfAbsent(tid, k -> new ArrayList<>()).add(it);
            }
        }

        BigDecimal freight = BigDecimal.ZERO;
        String p = normalize(province);
        for (Map.Entry<Long, List<Item>> e : groups.entrySet()) {
            freight = freight.add(templateFee(templates.get(e.getKey()), e.getValue(), p));
        }
        if (hasFallback) {
            // 没有任何模板可用的商品沿用旧规则：一口价运费，满门槛包邮（门槛已在上面按整单判断过）
            freight = freight.add(PriceCalculator.freight(total, global));
        }
        return scale(freight);
    }

    /** 单个模板的运费：先看指定包邮，再按匹配到的规则计首续费 */
    static BigDecimal templateFee(Template t, List<Item> items, String province) {
        int count = 0;
        BigDecimal weight = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;
        for (Item it : items) {
            count += it.quantity();
            weight = weight.add(nz(it.unitWeight()).multiply(BigDecimal.valueOf(it.quantity())));
            amount = amount.add(nz(it.amount()));
        }
        if (freeMatched(t.frees(), province, count, amount)) {
            return zero();
        }
        Rule rule = matchRule(t.rules(), province);
        if (rule == null) {
            if (province == null) {
                // 未选地址的预览：没有默认地区规则时无法估算，先按 0 展示，提交时再按真实地址校验
                return zero();
            }
            throw new BusinessException("收货地址不在「" + t.name() + "」的配送范围内");
        }
        BigDecimal units = t.chargeType() == ChargeType.BY_WEIGHT ? weight : BigDecimal.valueOf(count);
        return fee(rule, units);
    }

    /** 首续费：units ≤ 首件（首重）收首费；超出部分按续件（续重）向上取整，每档收一次续费 */
    static BigDecimal fee(Rule r, BigDecimal units) {
        BigDecimal fee = nz(r.firstFee());
        BigDecimal over = units.subtract(nz(r.firstUnit()));
        if (over.signum() > 0 && nz(r.nextUnit()).signum() > 0) {
            BigDecimal steps = over.divide(r.nextUnit(), 0, RoundingMode.CEILING);
            fee = fee.add(steps.multiply(nz(r.nextFee())));
        }
        return scale(fee);
    }

    /** 指定地区规则优先；都不命中时用默认地区（provinces 为空）规则 */
    static Rule matchRule(List<Rule> rules, String province) {
        if (rules == null) {
            return null;
        }
        Rule fallback = null;
        for (Rule r : rules) {
            if (r.provinces() == null || r.provinces().isEmpty()) {
                if (fallback == null) {
                    fallback = r;
                }
            } else if (province != null && containsProvince(r.provinces(), province)) {
                return r;
            }
        }
        return fallback;
    }

    private static boolean freeMatched(List<Free> frees, String province, int count, BigDecimal amount) {
        if (frees == null) {
            return false;
        }
        for (Free f : frees) {
            boolean regionHit = f.provinces() == null || f.provinces().isEmpty()
                    || (province != null && containsProvince(f.provinces(), province));
            if (!regionHit) {
                continue;
            }
            boolean byCount = f.minCount() > 0 && count >= f.minCount();
            boolean byAmount = nz(f.minAmount()).signum() > 0 && amount.compareTo(f.minAmount()) >= 0;
            if (byCount || byAmount) {
                return true;
            }
        }
        return false;
    }

    private static boolean globalFree(BigDecimal total, FreightRule global) {
        // 门槛 ≤ 0 在旧规则里表示「一律包邮」，只作用于没有模板的商品（见 PriceCalculator.freight），
        // 不能让它把所有模板运费都免掉，否则配置模板后一个历史参数就能让模板全部失效
        return global != null && global.freeThreshold() != null && global.freeThreshold().signum() > 0
                && total.compareTo(global.freeThreshold()) >= 0;
    }

    private static Long resolveTemplate(Long templateId, Map<Long, Template> templates, Long defaultTemplateId) {
        if (templateId != null && templates.containsKey(templateId)) {
            return templateId;
        }
        if (defaultTemplateId != null && templates.containsKey(defaultTemplateId)) {
            return defaultTemplateId;
        }
        return null;
    }

    private static boolean containsProvince(List<String> provinces, String normalized) {
        for (String p : provinces) {
            if (Objects.equals(normalize(p), normalized)) {
                return true;
            }
        }
        return false;
    }

    /** 统一省份写法：去空白与「省 / 市 / 自治区」等后缀，地址库与模板配置写法不同也能匹配 */
    public static String normalize(String province) {
        if (province == null) {
            return null;
        }
        String s = province.trim();
        if (s.isEmpty()) {
            return null;
        }
        for (String suffix : PROVINCE_SUFFIXES) {
            if (s.length() > suffix.length() && s.endsWith(suffix)) {
                return s.substring(0, s.length() - suffix.length());
            }
        }
        return s;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
