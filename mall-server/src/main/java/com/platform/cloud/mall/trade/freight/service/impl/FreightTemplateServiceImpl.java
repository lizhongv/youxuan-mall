package com.platform.cloud.mall.trade.freight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.trade.freight.dto.FreightTemplateQuery;
import com.platform.cloud.mall.trade.freight.dto.FreightTemplateRequest;
import com.platform.cloud.mall.trade.freight.dto.FreightTemplateRequest.FreeForm;
import com.platform.cloud.mall.trade.freight.dto.FreightTemplateRequest.RuleForm;
import com.platform.cloud.mall.trade.freight.entity.FreightTemplate;
import com.platform.cloud.mall.trade.freight.entity.FreightTemplateFree;
import com.platform.cloud.mall.trade.freight.entity.FreightTemplateRule;
import com.platform.cloud.mall.trade.freight.mapper.FreightTemplateFreeMapper;
import com.platform.cloud.mall.trade.freight.mapper.FreightTemplateMapper;
import com.platform.cloud.mall.trade.freight.mapper.FreightTemplateRuleMapper;
import com.platform.cloud.mall.trade.freight.service.FreightTemplateService;
import com.platform.cloud.mall.trade.freight.support.FreightCalculator;
import com.platform.cloud.mall.trade.freight.vo.FreightTemplateOptionVO;
import com.platform.cloud.mall.trade.freight.vo.FreightTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 运费模板维护。规则与包邮条件随模板整体提交，保存时先逻辑删除旧明细再插入新明细，
 * 比逐条比对更简单，且明细不被其他表引用，重建不影响历史订单（订单只存运费金额快照）。
 */
@Service
@RequiredArgsConstructor
public class FreightTemplateServiceImpl implements FreightTemplateService {

    private final FreightTemplateMapper templateMapper;
    private final FreightTemplateRuleMapper ruleMapper;
    private final FreightTemplateFreeMapper freeMapper;
    private final GoodsApi goodsApi;

    @Override
    public PageVO<FreightTemplateVO> page(FreightTemplateQuery q) {
        LambdaQueryWrapper<FreightTemplate> w = Wrappers.<FreightTemplate>lambdaQuery()
                .like(StringUtils.hasText(q.getKeyword()), FreightTemplate::getName,
                        q.getKeyword() == null ? null : q.getKeyword().trim())
                .eq(StringUtils.hasText(q.getChargeType()), FreightTemplate::getChargeType, q.getChargeType())
                .eq(q.getStatus() != null, FreightTemplate::getStatus, q.getStatus())
                .orderByDesc(FreightTemplate::getIsDefault).orderByDesc(FreightTemplate::getId);
        Page<FreightTemplate> page = templateMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w);
        List<Long> ids = page.getRecords().stream().map(FreightTemplate::getId).toList();
        Map<Long, List<FreightTemplateRule>> rules = rulesOf(ids);
        Map<Long, List<FreightTemplateFree>> frees = freesOf(ids);
        return PageVO.of(page, t -> toVO(t, rules.getOrDefault(t.getId(), List.of()),
                frees.getOrDefault(t.getId(), List.of())));
    }

    @Override
    public FreightTemplateVO get(Long id) {
        FreightTemplate t = mustGet(id);
        return toVO(t, rulesOf(List.of(id)).getOrDefault(id, List.of()), freesOf(List.of(id)).getOrDefault(id, List.of()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(FreightTemplateRequest req) {
        validate(req);
        FreightTemplate t = new FreightTemplate();
        apply(t, req);
        templateMapper.insert(t);
        if (t.getIsDefault() == 1) {
            clearOtherDefaults(t.getId());
        }
        saveDetails(t.getId(), req);
        return t.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FreightTemplateRequest req) {
        FreightTemplate t = mustGet(id);
        validate(req);
        apply(t, req);
        if (templateMapper.updateById(t) == 0) {
            throw new BusinessException("数据已被修改，请刷新后重试");
        }
        if (t.getIsDefault() == 1) {
            clearOtherDefaults(id);
        }
        ruleMapper.delete(Wrappers.<FreightTemplateRule>lambdaQuery().eq(FreightTemplateRule::getTemplateId, id));
        freeMapper.delete(Wrappers.<FreightTemplateFree>lambdaQuery().eq(FreightTemplateFree::getTemplateId, id));
        saveDetails(id, req);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        mustGet(id);
        long used = goodsApi.countByFreightTemplate(id);
        if (used > 0) {
            throw new BusinessException("有 " + used + " 个商品正在使用该模板，请先为这些商品更换模板");
        }
        templateMapper.deleteById(id);
        ruleMapper.delete(Wrappers.<FreightTemplateRule>lambdaQuery().eq(FreightTemplateRule::getTemplateId, id));
        freeMapper.delete(Wrappers.<FreightTemplateFree>lambdaQuery().eq(FreightTemplateFree::getTemplateId, id));
    }

    @Override
    public List<FreightTemplateOptionVO> listOptions() {
        return templateMapper.selectList(Wrappers.<FreightTemplate>lambdaQuery()
                        .eq(FreightTemplate::getStatus, 1)
                        .orderByDesc(FreightTemplate::getIsDefault).orderByDesc(FreightTemplate::getId))
                .stream()
                .map(t -> new FreightTemplateOptionVO(t.getId(), t.getName(), t.getChargeType(), t.getIsDefault()))
                .toList();
    }

    /**
     * 业务校验：同一省份不能出现在两条计费规则里（否则匹配结果取决于顺序，运营难以理解）；
     * 默认地区规则最多一条；包邮条件至少设置件数或金额之一。
     */
    static void validate(FreightTemplateRequest req) {
        Set<String> seen = new HashSet<>();
        int defaults = 0;
        for (RuleForm r : req.getRules()) {
            List<String> ps = cleanProvinces(r.getProvinces());
            if (ps.isEmpty()) {
                defaults++;
            }
            for (String p : ps) {
                if (!seen.add(FreightCalculator.normalize(p))) {
                    throw new BusinessException("省份「" + p + "」在多条计费规则中重复");
                }
            }
        }
        if (defaults > 1) {
            throw new BusinessException("默认地区（全国其他地区）规则只能有一条");
        }
        if (req.getFrees() != null) {
            for (FreeForm f : req.getFrees()) {
                boolean byCount = f.getMinCount() != null && f.getMinCount() > 0;
                boolean byAmount = f.getMinAmount() != null && f.getMinAmount().signum() > 0;
                if (!byCount && !byAmount) {
                    throw new BusinessException("包邮条件需设置满件数或满金额");
                }
            }
        }
    }

    private static void apply(FreightTemplate t, FreightTemplateRequest req) {
        t.setName(req.getName().trim());
        t.setChargeType(req.getChargeType());
        t.setIsDefault(req.getIsDefault() != null && req.getIsDefault() == 1 ? 1 : 0);
        t.setStatus(req.getStatus() == null ? 1 : req.getStatus());
    }

    /** 默认模板全局唯一：新设默认时取消其他模板的默认标记 */
    private void clearOtherDefaults(Long selfId) {
        templateMapper.update(null, Wrappers.<FreightTemplate>lambdaUpdate()
                .eq(FreightTemplate::getIsDefault, 1)
                .ne(FreightTemplate::getId, selfId)
                .set(FreightTemplate::getIsDefault, 0));
    }

    private void saveDetails(Long templateId, FreightTemplateRequest req) {
        int sort = 0;
        for (RuleForm f : req.getRules()) {
            FreightTemplateRule r = new FreightTemplateRule();
            r.setTemplateId(templateId);
            r.setProvinces(cleanProvinces(f.getProvinces()));
            r.setFirstUnit(unit(f.getFirstUnit()));
            r.setFirstFee(money(f.getFirstFee()));
            r.setNextUnit(unit(f.getNextUnit()));
            r.setNextFee(money(f.getNextFee()));
            r.setSort(sort++);
            ruleMapper.insert(r);
        }
        if (req.getFrees() == null) {
            return;
        }
        for (FreeForm f : req.getFrees()) {
            FreightTemplateFree e = new FreightTemplateFree();
            e.setTemplateId(templateId);
            e.setProvinces(cleanProvinces(f.getProvinces()));
            e.setMinCount(f.getMinCount() == null ? 0 : Math.max(f.getMinCount(), 0));
            e.setMinAmount(money(f.getMinAmount()));
            freeMapper.insert(e);
        }
    }

    private Map<Long, List<FreightTemplateRule>> rulesOf(List<Long> ids) {
        Map<Long, List<FreightTemplateRule>> m = new HashMap<>();
        if (ids.isEmpty()) {
            return m;
        }
        ruleMapper.selectList(Wrappers.<FreightTemplateRule>lambdaQuery()
                        .in(FreightTemplateRule::getTemplateId, ids)
                        .orderByAsc(FreightTemplateRule::getSort).orderByAsc(FreightTemplateRule::getId))
                .forEach(r -> m.computeIfAbsent(r.getTemplateId(), k -> new ArrayList<>()).add(r));
        return m;
    }

    private Map<Long, List<FreightTemplateFree>> freesOf(List<Long> ids) {
        Map<Long, List<FreightTemplateFree>> m = new HashMap<>();
        if (ids.isEmpty()) {
            return m;
        }
        freeMapper.selectList(Wrappers.<FreightTemplateFree>lambdaQuery()
                        .in(FreightTemplateFree::getTemplateId, ids)
                        .orderByAsc(FreightTemplateFree::getId))
                .forEach(f -> m.computeIfAbsent(f.getTemplateId(), k -> new ArrayList<>()).add(f));
        return m;
    }

    private FreightTemplate mustGet(Long id) {
        FreightTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException("运费模板不存在");
        }
        return t;
    }

    private static FreightTemplateVO toVO(FreightTemplate t, List<FreightTemplateRule> rules,
                                          List<FreightTemplateFree> frees) {
        FreightTemplateVO vo = new FreightTemplateVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setChargeType(t.getChargeType());
        vo.setIsDefault(t.getIsDefault());
        vo.setStatus(t.getStatus());
        vo.setCreatedAt(t.getCreatedAt());
        vo.setRules(rules.stream().map(r -> {
            FreightTemplateVO.RuleVO v = new FreightTemplateVO.RuleVO();
            v.setProvinces(r.getProvinces() == null ? List.of() : r.getProvinces());
            v.setFirstUnit(r.getFirstUnit());
            v.setFirstFee(r.getFirstFee());
            v.setNextUnit(r.getNextUnit());
            v.setNextFee(r.getNextFee());
            return v;
        }).toList());
        vo.setFrees(frees.stream().map(f -> {
            FreightTemplateVO.FreeVO v = new FreightTemplateVO.FreeVO();
            v.setProvinces(f.getProvinces() == null ? List.of() : f.getProvinces());
            v.setMinCount(f.getMinCount());
            v.setMinAmount(f.getMinAmount());
            return v;
        }).toList());
        return vo;
    }

    /** 去空白、去空项、去重（保留顺序） */
    static List<String> cleanProvinces(List<String> ps) {
        if (ps == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(ps.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList());
    }

    private static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal unit(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(3, RoundingMode.HALF_UP);
    }
}
