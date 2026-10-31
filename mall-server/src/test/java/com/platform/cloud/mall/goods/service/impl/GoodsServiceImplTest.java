package com.platform.cloud.mall.goods.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.platform.cloud.mall.goods.dto.GoodsForm;
import com.platform.cloud.mall.goods.dto.GoodsForm.SkuForm;
import com.platform.cloud.mall.goods.entity.Goods;
import com.platform.cloud.mall.goods.entity.Sku;
import com.platform.cloud.mall.goods.entity.SpecItem;
import com.platform.cloud.mall.goods.mapper.SkuMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品保存：SPU 汇总写入与 SKU 按 id 对齐（更新 / 新增 / 删除）。
 */
@ExtendWith(MockitoExtension.class)
class GoodsServiceImplTest {

    private static final long GOODS_ID = 100L;
    private static final List<SpecItem> SPECS = List.of(new SpecItem("颜色", List.of("红", "蓝", "绿")));

    @Mock
    private SkuMapper skuMapper;
    @InjectMocks
    private GoodsServiceImpl service;

    /** Lambda 条件需要实体元数据，单测里手动注册，避免启动 Spring */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Sku.class);
        TableInfoHelper.initTableInfo(assistant, Goods.class);
    }

    private static Sku existing(long id, String color) {
        Sku s = new Sku();
        s.setId(id);
        s.setGoodsId(GOODS_ID);
        s.setSpecValues(new ArrayList<>(List.of(color)));
        s.setPrice(BigDecimal.TEN);
        s.setStock(5);
        s.setSales(3);
        s.setVersion(1);
        return s;
    }

    private static SkuForm form(Long id, String color, String price, int stock) {
        SkuForm f = new SkuForm();
        f.setId(id);
        f.setSpecValues(new ArrayList<>(List.of(color)));
        f.setPrice(new BigDecimal(price));
        f.setOriginalPrice(new BigDecimal("199"));
        f.setStock(stock);
        return f;
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<LambdaUpdateWrapper<Sku>> skuWrapper() {
        return ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void syncUpdatesInsertsAndDeletes() {
        when(skuMapper.selectList(any())).thenReturn(List.of(existing(1L, "红"), existing(2L, "蓝")));
        when(skuMapper.update(isNull(), any())).thenReturn(1);
        when(skuMapper.adjustStock(anyLong(), anyInt())).thenReturn(1);
        SkuForm red = form(1L, "红", "88.888", 20);
        red.setOriginStock(5);

        service.syncSkus(GOODS_ID, List.of(red, form(null, "绿", "99", 7)), SPECS);

        ArgumentCaptor<LambdaUpdateWrapper<Sku>> w = skuWrapper();
        verify(skuMapper).update(isNull(), w.capture());
        // 已有 SKU 只写表单字段：不写库存、不写销量
        assertThat(w.getValue().getSqlSet()).contains("price", "spec_text", "weight")
                .doesNotContain("stock").doesNotContain("sales");
        assertThat(w.getValue().getParamNameValuePairs().values()).contains(new BigDecimal("88.89"), "颜色:红");
        // 目标 20 − 打开时 5 = +15，按差量条件更新
        verify(skuMapper).adjustStock(1L, 15);
        verify(skuMapper, never()).updateById(any(Sku.class));

        ArgumentCaptor<Sku> inserted = ArgumentCaptor.forClass(Sku.class);
        verify(skuMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getGoodsId()).isEqualTo(GOODS_ID);
        assertThat(inserted.getValue().getSpecText()).isEqualTo("颜色:绿");
        assertThat(inserted.getValue().getSales()).isZero();

        ArgumentCaptor<Collection<Long>> removed = ArgumentCaptor.forClass(Collection.class);
        verify(skuMapper).deleteBatchIds(removed.capture());
        assertThat(removed.getValue()).containsExactly(2L);
    }

    @Test
    void rejectsSkuOfOtherGoods() {
        when(skuMapper.selectList(any())).thenReturn(List.of(existing(1L, "红")));
        assertThatThrownBy(() -> service.syncSkus(GOODS_ID, List.of(form(999L, "红", "1", 1)), SPECS))
                .hasMessage("SKU 不存在或不属于该商品");
    }

    @Test
    void 编辑期间被下单买走_减库存后小于0则失败() {
        when(skuMapper.selectList(any())).thenReturn(List.of(existing(1L, "红")));
        when(skuMapper.update(isNull(), any())).thenReturn(1);
        when(skuMapper.adjustStock(1L, -5)).thenReturn(0);
        SkuForm f = form(1L, "红", "1", 0);
        f.setOriginStock(5);
        assertThatThrownBy(() -> service.syncSkus(GOODS_ID, List.of(f), SPECS))
                .hasMessageContaining("请刷新后重试");
    }

    @Test
    void 库存未改动或旧前端未传原库存时不调整库存() {
        when(skuMapper.selectList(any())).thenReturn(List.of(existing(1L, "红"), existing(2L, "蓝")));
        when(skuMapper.update(isNull(), any())).thenReturn(1);
        SkuForm same = form(1L, "红", "1", 5);
        same.setOriginStock(5);
        SkuForm legacy = form(2L, "蓝", "1", 999);

        service.syncSkus(GOODS_ID, List.of(same, legacy), SPECS);

        verify(skuMapper, never()).adjustStock(anyLong(), anyInt());
    }

    @Test
    void 差量计算() {
        SkuForm f = form(1L, "红", "1", 8);
        assertThat(GoodsServiceImpl.stockDelta(f)).isZero();
        f.setOriginStock(10);
        assertThat(GoodsServiceImpl.stockDelta(f)).isEqualTo(-2);
    }

    @Test
    void SKU已被删除时失败() {
        when(skuMapper.selectList(any())).thenReturn(List.of(existing(1L, "红")));
        when(skuMapper.update(isNull(), any())).thenReturn(0);
        assertThatThrownBy(() -> service.syncSkus(GOODS_ID, List.of(form(1L, "红", "1", 1)), SPECS))
                .hasMessageContaining("请刷新后重试");
    }

    @Test
    void 商品表单字段不含库存与销量() {
        Goods g = new Goods();
        g.setName("x");
        g.setFreightTemplateId(7L);
        String set = GoodsServiceImpl.formFields(g).getSqlSet();
        assertThat(set).contains("freight_template_id", "name", "price")
                .doesNotContain("stock").doesNotContainPattern("(^|,)sales=");
    }

    @Test
    void nothingRemovedWhenAllKept() {
        when(skuMapper.selectList(any())).thenReturn(List.of(existing(1L, "红")));
        when(skuMapper.update(isNull(), any())).thenReturn(1);
        service.syncSkus(GOODS_ID, List.of(form(1L, "红", "1", 1)), SPECS);
        verify(skuMapper, never()).deleteBatchIds(any());
    }

    @Test
    void applySummarizesFromSkus() {
        GoodsForm f = new GoodsForm();
        f.setCategoryId(1L);
        f.setName(" 测试商品 ");
        f.setPic("/a.jpg");
        f.setSkus(List.of(form(null, "红", "120", 3), form(null, "蓝", "99.5", 4)));
        Goods g = new Goods();
        GoodsServiceImpl.apply(g, f, SPECS);
        assertThat(g.getName()).isEqualTo("测试商品");
        assertThat(g.getPrice()).isEqualByComparingTo("99.50");
        assertThat(g.getOriginalPrice()).isEqualByComparingTo("199.00");
        assertThat(g.getStock()).isEqualTo(7);
        assertThat(g.getStatus()).isEqualTo("OFF_SALE");
        assertThat(g.getUnit()).isEqualTo("件");
        assertThat(g.getIsNew()).isZero();
    }
}
