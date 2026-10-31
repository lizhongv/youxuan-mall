package com.platform.cloud.mall.trade.logistics.support;

import com.platform.cloud.mall.system.service.SysDictService;
import com.platform.cloud.mall.system.vo.DictItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 把订单里保存的快递公司解析为快递100 编码。
 * <p>
 * 历史原因订单 delivery_company 存的是字典 label（中文名，后台发货下拉框提交 label），
 * V7 之后字典 value 改成了快递100 编码，前端也可能改为提交 value。为兼容新旧数据，按以下顺序匹配：
 * <ol>
 *   <li>字典 value（快递100 编码，忽略大小写）</li>
 *   <li>字典 label（中文名）</li>
 *   <li>V2 种子里的旧编码（SF / ZTO …），防止有人按旧 value 录入</li>
 *   <li>内置常用名称与简称（字典被改名或删除时兜底）</li>
 *   <li>本身就是小写字母数字：认为是后台手填的快递100 编码，原样使用</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpressCompanyResolver {

    public static final String DICT_TYPE = "express_company";

    /** V2 种子数据里的旧编码 → 快递100 编码 */
    static final Map<String, String> LEGACY_CODES = Map.of(
            "SF", "shunfeng", "ZTO", "zhongtong", "YTO", "yuantong", "YD", "yunda",
            "STO", "shentong", "JD", "jd", "EMS", "ems", "JTSD", "jtexpress");

    /** 内置名称兜底，覆盖种子 label 与常见简称 */
    static final Map<String, ExpressCompany> BUILTIN_NAMES = Map.ofEntries(
            Map.entry("顺丰速运", new ExpressCompany("shunfeng", "顺丰速运")),
            Map.entry("顺丰", new ExpressCompany("shunfeng", "顺丰速运")),
            Map.entry("中通快递", new ExpressCompany("zhongtong", "中通快递")),
            Map.entry("中通", new ExpressCompany("zhongtong", "中通快递")),
            Map.entry("圆通速递", new ExpressCompany("yuantong", "圆通速递")),
            Map.entry("圆通", new ExpressCompany("yuantong", "圆通速递")),
            Map.entry("韵达快递", new ExpressCompany("yunda", "韵达快递")),
            Map.entry("韵达", new ExpressCompany("yunda", "韵达快递")),
            Map.entry("申通快递", new ExpressCompany("shentong", "申通快递")),
            Map.entry("申通", new ExpressCompany("shentong", "申通快递")),
            Map.entry("京东物流", new ExpressCompany("jd", "京东物流")),
            Map.entry("京东", new ExpressCompany("jd", "京东物流")),
            Map.entry("EMS", new ExpressCompany("ems", "EMS")),
            Map.entry("邮政EMS", new ExpressCompany("ems", "EMS")),
            Map.entry("极兔速递", new ExpressCompany("jtexpress", "极兔速递")),
            Map.entry("极兔", new ExpressCompany("jtexpress", "极兔速递")));

    private static final Pattern KUAIDI100_CODE = Pattern.compile("^[a-z0-9]{2,32}$");

    private final SysDictService dictService;

    /** 解析失败返回 empty，调用方提示「暂不支持查询该快递公司」 */
    public Optional<ExpressCompany> resolve(String stored) {
        if (stored == null || stored.isBlank()) {
            return Optional.empty();
        }
        String s = stored.trim();
        List<DictItemVO> items = loadDict();

        for (DictItemVO d : items) {
            if (d.getValue() != null && d.getValue().equalsIgnoreCase(s)) {
                return Optional.of(new ExpressCompany(normalizeCode(d.getValue()), d.getLabel()));
            }
        }
        for (DictItemVO d : items) {
            if (s.equals(d.getLabel())) {
                return Optional.of(new ExpressCompany(normalizeCode(d.getValue()), d.getLabel()));
            }
        }
        String legacy = LEGACY_CODES.get(s.toUpperCase(Locale.ROOT));
        if (legacy != null) {
            return Optional.of(new ExpressCompany(legacy, labelOf(items, legacy).orElse(s)));
        }
        ExpressCompany builtin = BUILTIN_NAMES.get(s);
        if (builtin != null) {
            return Optional.of(builtin);
        }
        if (KUAIDI100_CODE.matcher(s).matches()) {
            return Optional.of(new ExpressCompany(s, labelOf(items, s).orElse(s)));
        }
        return Optional.empty();
    }

    /** 字典值若仍是旧编码（未执行 V7 或被手工改回），也转换成快递100 编码 */
    private static String normalizeCode(String value) {
        String v = value.trim();
        String legacy = LEGACY_CODES.get(v.toUpperCase(Locale.ROOT));
        return legacy != null ? legacy : v.toLowerCase(Locale.ROOT);
    }

    private static Optional<String> labelOf(List<DictItemVO> items, String code) {
        return items.stream()
                .filter(d -> d.getValue() != null && normalizeCode(d.getValue()).equals(code))
                .map(DictItemVO::getLabel)
                .findFirst();
    }

    /** 字典读取失败不应让物流查询整体失败，退回内置映射 */
    private List<DictItemVO> loadDict() {
        try {
            List<DictItemVO> items = dictService.items(DICT_TYPE);
            return items == null ? List.of() : items;
        } catch (RuntimeException e) {
            log.warn("读取快递公司字典失败，使用内置映射：{}", e.getMessage());
            return List.of();
        }
    }
}
