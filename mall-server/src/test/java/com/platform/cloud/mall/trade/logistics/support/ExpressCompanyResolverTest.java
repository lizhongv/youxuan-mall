package com.platform.cloud.mall.trade.logistics.support;

import com.platform.cloud.mall.system.service.SysDictService;
import com.platform.cloud.mall.system.vo.DictItemVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 快递公司解析：新编码、历史中文名、旧种子编码、字典不可用时的兜底。
 */
class ExpressCompanyResolverTest {

    private static final List<DictItemVO> DICT = List.of(
            new DictItemVO("顺丰速运", "shunfeng"),
            new DictItemVO("中通快递", "zhongtong"),
            new DictItemVO("极兔速递", "jtexpress"));

    private ExpressCompanyResolver resolver(List<DictItemVO> items) {
        SysDictService dict = mock(SysDictService.class);
        when(dict.items(ExpressCompanyResolver.DICT_TYPE)).thenReturn(items);
        return new ExpressCompanyResolver(dict);
    }

    @Test
    void matchesDictValue() {
        assertThat(resolver(DICT).resolve("shunfeng")).contains(new ExpressCompany("shunfeng", "顺丰速运"));
        assertThat(resolver(DICT).resolve("ZHONGTONG")).contains(new ExpressCompany("zhongtong", "中通快递"));
    }

    @Test
    void historicalOrdersStoreChineseLabel() {
        assertThat(resolver(DICT).resolve(" 极兔速递 ")).contains(new ExpressCompany("jtexpress", "极兔速递"));
    }

    @Test
    void legacySeedCodesAreMapped() {
        assertThat(resolver(DICT).resolve("SF")).contains(new ExpressCompany("shunfeng", "顺丰速运"));
        assertThat(resolver(DICT).resolve("JTSD")).contains(new ExpressCompany("jtexpress", "极兔速递"));
    }

    @Test
    void dictStillHoldingLegacyValuesIsNormalized() {
        List<DictItemVO> old = List.of(new DictItemVO("韵达快递", "YD"));
        assertThat(resolver(old).resolve("韵达快递")).contains(new ExpressCompany("yunda", "韵达快递"));
    }

    @Test
    void builtinNamesCoverMissingOrFailingDict() {
        assertThat(resolver(List.of()).resolve("圆通速递")).contains(new ExpressCompany("yuantong", "圆通速递"));
        SysDictService broken = mock(SysDictService.class);
        when(broken.items(ExpressCompanyResolver.DICT_TYPE)).thenThrow(new RuntimeException("db down"));
        assertThat(new ExpressCompanyResolver(broken).resolve("京东")).contains(new ExpressCompany("jd", "京东物流"));
    }

    @Test
    void rawKuaidi100CodePassesThroughAndUnknownNameFails() {
        assertThat(resolver(DICT).resolve("debangkuaidi")).contains(new ExpressCompany("debangkuaidi", "debangkuaidi"));
        assertThat(resolver(DICT).resolve("某某快递")).isEmpty();
        assertThat(resolver(DICT).resolve(null)).isEmpty();
    }

    @Test
    void onlyShunfengRequiresPhone() {
        assertThat(new ExpressCompany("shunfeng", "顺丰速运").requiresPhone()).isTrue();
        assertThat(new ExpressCompany("zhongtong", "中通快递").requiresPhone()).isFalse();
    }
}
