package com.platform.cloud.mall.trade.freight.service.impl;

import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.trade.MpTestSupport;
import com.platform.cloud.mall.trade.common.TradeSettings;
import com.platform.cloud.mall.trade.freight.dto.FreightTemplateRequest;
import com.platform.cloud.mall.trade.freight.dto.FreightTemplateRequest.FreeForm;
import com.platform.cloud.mall.trade.freight.dto.FreightTemplateRequest.RuleForm;
import com.platform.cloud.mall.trade.freight.entity.FreightTemplate;
import com.platform.cloud.mall.trade.freight.entity.FreightTemplateFree;
import com.platform.cloud.mall.trade.freight.entity.FreightTemplateRule;
import com.platform.cloud.mall.trade.freight.mapper.FreightTemplateFreeMapper;
import com.platform.cloud.mall.trade.freight.mapper.FreightTemplateMapper;
import com.platform.cloud.mall.trade.freight.mapper.FreightTemplateRuleMapper;
import com.platform.cloud.mall.trade.freight.support.FreightCalculator;
import com.platform.cloud.mall.trade.order.service.PriceCalculator.FreightRule;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 运费模板维护（校验、默认模板互斥、删除保护）与报价（加载启用模板 + 默认模板）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FreightTemplateServiceImplTest {

    @Mock
    FreightTemplateMapper templateMapper;
    @Mock
    FreightTemplateRuleMapper ruleMapper;
    @Mock
    FreightTemplateFreeMapper freeMapper;
    @Mock
    GoodsApi goodsApi;
    @Mock
    TradeSettings settings;

    @BeforeAll
    static void init() {
        MpTestSupport.init(FreightTemplate.class, FreightTemplateRule.class, FreightTemplateFree.class);
    }

    private FreightTemplateServiceImpl service() {
        return new FreightTemplateServiceImpl(templateMapper, ruleMapper, freeMapper, goodsApi);
    }

    private static RuleForm rule(List<String> provinces, String first, String fee) {
        RuleForm r = new RuleForm();
        r.setProvinces(new ArrayList<>(provinces));
        r.setFirstUnit(new BigDecimal(first));
        r.setFirstFee(new BigDecimal(fee));
        r.setNextUnit(BigDecimal.ONE);
        r.setNextFee(BigDecimal.ONE);
        return r;
    }

    private static FreightTemplateRequest req(List<RuleForm> rules) {
        FreightTemplateRequest r = new FreightTemplateRequest();
        r.setName(" 普通快递 ");
        r.setChargeType("BY_COUNT");
        r.setIsDefault(1);
        r.setRules(rules);
        return r;
    }

    @Test
    void 新建默认模板_取消其他默认_保存规则与包邮条件() {
        FreeForm free = new FreeForm();
        free.setProvinces(List.of("广东省"));
        free.setMinCount(3);
        FreightTemplateRequest r = req(List.of(rule(List.of(" 广东省 ", "", "广东省"), "1", "8"),
                rule(List.of(), "1", "10")));
        r.setFrees(List.of(free));

        service().create(r);

        ArgumentCaptor<FreightTemplate> t = ArgumentCaptor.forClass(FreightTemplate.class);
        verify(templateMapper).insert(t.capture());
        assertThat(t.getValue().getName()).isEqualTo("普通快递");
        assertThat(t.getValue().getIsDefault()).isEqualTo(1);
        verify(templateMapper).update(isNull(), any());
        ArgumentCaptor<FreightTemplateRule> rules = ArgumentCaptor.forClass(FreightTemplateRule.class);
        verify(ruleMapper, times(2)).insert(rules.capture());
        // 省份去空白、去空项、去重
        assertThat(rules.getAllValues().get(0).getProvinces()).containsExactly("广东省");
        assertThat(rules.getAllValues().get(0).getFirstFee()).isEqualByComparingTo("8.00");
        verify(freeMapper).insert(any(FreightTemplateFree.class));
    }

    @Test
    void 同一省份出现在两条规则中被拒绝_写法不同也算重复() {
        FreightTemplateRequest r = req(List.of(rule(List.of("广西壮族自治区"), "1", "8"),
                rule(List.of("广西"), "1", "9")));
        assertThatThrownBy(() -> service().create(r)).hasMessageContaining("重复");
        verify(templateMapper, never()).insert(any(FreightTemplate.class));
    }

    @Test
    void 默认地区规则只能一条() {
        FreightTemplateRequest r = req(List.of(rule(List.of(), "1", "8"), rule(List.of(), "1", "9")));
        assertThatThrownBy(() -> service().create(r)).hasMessageContaining("只能有一条");
    }

    @Test
    void 包邮条件需设置件数或金额() {
        FreightTemplateRequest r = req(List.of(rule(List.of(), "1", "8")));
        r.setFrees(List.of(new FreeForm()));
        assertThatThrownBy(() -> service().create(r)).hasMessageContaining("满件数或满金额");
    }

    @Test
    void 有商品引用时不能删除() {
        FreightTemplate t = new FreightTemplate();
        t.setId(5L);
        when(templateMapper.selectById(5L)).thenReturn(t);
        when(goodsApi.countByFreightTemplate(5L)).thenReturn(3L);

        assertThatThrownBy(() -> service().delete(5L)).hasMessageContaining("3 个商品");
        verify(templateMapper, never()).deleteById(5L);
    }

    @Test
    void 修改时整体替换规则() {
        FreightTemplate t = new FreightTemplate();
        t.setId(5L);
        when(templateMapper.selectById(5L)).thenReturn(t);
        when(templateMapper.updateById(any(FreightTemplate.class))).thenReturn(1);
        FreightTemplateRequest r = req(List.of(rule(List.of(), "1", "8")));
        r.setIsDefault(0);

        service().update(5L, r);

        verify(ruleMapper).delete(any());
        verify(freeMapper).delete(any());
        verify(ruleMapper).insert(any(FreightTemplateRule.class));
        // 非默认模板不去动其他模板的默认标记
        verify(templateMapper, never()).update(isNull(), any());
    }

    @Test
    void 报价加载商品模板与默认模板() {
        FreightTemplate def = new FreightTemplate();
        def.setId(1L);
        def.setName("默认");
        def.setChargeType("BY_COUNT");
        Page<FreightTemplate> page = new Page<>();
        page.setRecords(List.of(def));
        when(templateMapper.selectPage(any(), any())).thenReturn(page);
        when(templateMapper.selectList(any())).thenReturn(List.of(def));
        FreightTemplateRule rule = new FreightTemplateRule();
        rule.setTemplateId(1L);
        rule.setProvinces(List.of());
        rule.setFirstUnit(BigDecimal.ONE);
        rule.setFirstFee(new BigDecimal("6"));
        rule.setNextUnit(BigDecimal.ONE);
        rule.setNextFee(new BigDecimal("2"));
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule));
        when(freeMapper.selectList(any())).thenReturn(List.of());
        when(settings.freightRule()).thenReturn(new FreightRule(new BigDecimal("10"), new BigDecimal("99")));
        FreightQuoteServiceImpl quote = new FreightQuoteServiceImpl(templateMapper, ruleMapper, freeMapper, settings);

        // 商品未指定模板 → 默认模板：2 件 = 6 + 2
        BigDecimal f = quote.quote("广东省", List.of(new FreightCalculator.Item(null, 2, null, new BigDecimal("20"))));

        assertThat(f).isEqualByComparingTo("8.00");
    }

    @Test
    void 没有任何模板时回退全局一口价() {
        when(templateMapper.selectPage(any(), any())).thenReturn(new Page<>());
        when(settings.freightRule()).thenReturn(new FreightRule(new BigDecimal("10"), new BigDecimal("99")));
        FreightQuoteServiceImpl quote = new FreightQuoteServiceImpl(templateMapper, ruleMapper, freeMapper, settings);

        BigDecimal f = quote.quote("广东省", List.of(new FreightCalculator.Item(null, 1, null, new BigDecimal("20"))));

        assertThat(f).isEqualByComparingTo("10.00");
        verify(templateMapper, never()).selectList(any());
    }
}
