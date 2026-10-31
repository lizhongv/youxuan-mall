package com.platform.cloud.mall.trade.freight.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.mall.trade.common.TradeSettings;
import com.platform.cloud.mall.trade.freight.entity.FreightTemplate;
import com.platform.cloud.mall.trade.freight.entity.FreightTemplateFree;
import com.platform.cloud.mall.trade.freight.entity.FreightTemplateRule;
import com.platform.cloud.mall.trade.freight.mapper.FreightTemplateFreeMapper;
import com.platform.cloud.mall.trade.freight.mapper.FreightTemplateMapper;
import com.platform.cloud.mall.trade.freight.mapper.FreightTemplateRuleMapper;
import com.platform.cloud.mall.trade.freight.service.FreightQuoteService;
import com.platform.cloud.mall.trade.freight.support.FreightCalculator;
import com.platform.cloud.mall.trade.freight.support.FreightCalculator.ChargeType;
import com.platform.cloud.mall.trade.freight.support.FreightCalculator.Free;
import com.platform.cloud.mall.trade.freight.support.FreightCalculator.Item;
import com.platform.cloud.mall.trade.freight.support.FreightCalculator.Rule;
import com.platform.cloud.mall.trade.freight.support.FreightCalculator.Template;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 运费报价：只读，三次批量查询（模板、规则、包邮条件），与商品数量无关。
 */
@Service
@RequiredArgsConstructor
public class FreightQuoteServiceImpl implements FreightQuoteService {

    private final FreightTemplateMapper templateMapper;
    private final FreightTemplateRuleMapper ruleMapper;
    private final FreightTemplateFreeMapper freeMapper;
    private final TradeSettings settings;

    @Override
    public BigDecimal quote(String province, List<Item> items) {
        Set<Long> ids = items.stream().map(Item::templateId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        FreightTemplate def = defaultTemplate(templateMapper);
        if (def != null) {
            ids.add(def.getId());
        }
        Map<Long, Template> templates = ids.isEmpty() ? Map.of() : load(ids);
        return FreightCalculator.calculate(province, items, templates, def == null ? null : def.getId(),
                settings.freightRule());
    }

    /** 启用中的默认模板；保存时已保证唯一，这里仍按 ID 倒序取 1 条兜底脏数据 */
    static FreightTemplate defaultTemplate(FreightTemplateMapper mapper) {
        List<FreightTemplate> list = mapper.selectPage(new Page<>(1, 1, false),
                Wrappers.<FreightTemplate>lambdaQuery()
                        .eq(FreightTemplate::getIsDefault, 1)
                        .eq(FreightTemplate::getStatus, 1)
                        .orderByDesc(FreightTemplate::getId)).getRecords();
        return list.isEmpty() ? null : list.get(0);
    }

    /** 只加载启用中的模板：停用的模板视同不存在，商品回退到默认模板 */
    private Map<Long, Template> load(Set<Long> ids) {
        List<FreightTemplate> list = templateMapper.selectList(Wrappers.<FreightTemplate>lambdaQuery()
                .in(FreightTemplate::getId, ids)
                .eq(FreightTemplate::getStatus, 1));
        if (list.isEmpty()) {
            return Map.of();
        }
        List<Long> enabled = list.stream().map(FreightTemplate::getId).toList();
        Map<Long, List<Rule>> rules = new HashMap<>();
        ruleMapper.selectList(Wrappers.<FreightTemplateRule>lambdaQuery()
                        .in(FreightTemplateRule::getTemplateId, enabled)
                        .orderByAsc(FreightTemplateRule::getSort).orderByAsc(FreightTemplateRule::getId))
                .forEach(r -> rules.computeIfAbsent(r.getTemplateId(), k -> new ArrayList<>()).add(toRule(r)));
        Map<Long, List<Free>> frees = new HashMap<>();
        freeMapper.selectList(Wrappers.<FreightTemplateFree>lambdaQuery()
                        .in(FreightTemplateFree::getTemplateId, enabled))
                .forEach(f -> frees.computeIfAbsent(f.getTemplateId(), k -> new ArrayList<>()).add(toFree(f)));
        Map<Long, Template> result = new HashMap<>();
        for (FreightTemplate t : list) {
            result.put(t.getId(), new Template(t.getId(), t.getName(), ChargeType.valueOf(t.getChargeType()),
                    rules.getOrDefault(t.getId(), List.of()), frees.getOrDefault(t.getId(), List.of())));
        }
        return result;
    }

    static Rule toRule(FreightTemplateRule r) {
        return new Rule(r.getProvinces() == null ? List.of() : r.getProvinces(), r.getFirstUnit(), r.getFirstFee(),
                r.getNextUnit(), r.getNextFee());
    }

    static Free toFree(FreightTemplateFree f) {
        return new Free(f.getProvinces() == null ? List.of() : f.getProvinces(),
                f.getMinCount() == null ? 0 : f.getMinCount(), f.getMinAmount());
    }
}
