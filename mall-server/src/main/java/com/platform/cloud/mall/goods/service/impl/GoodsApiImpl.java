package com.platform.cloud.mall.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.goods.entity.Goods;
import com.platform.cloud.mall.goods.entity.Sku;
import com.platform.cloud.mall.goods.mapper.GoodsMapper;
import com.platform.cloud.mall.goods.mapper.SkuMapper;
import com.platform.cloud.mall.goods.support.GoodsConverter;
import com.platform.cloud.mall.goods.support.GoodsStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品领域对外能力实现。只依赖本领域 Mapper，不依赖其他领域，避免循环依赖。
 */
@Service
@RequiredArgsConstructor
public class GoodsApiImpl implements GoodsApi {

    private final GoodsMapper goodsMapper;
    private final SkuMapper skuMapper;

    @Override
    public Map<Long, SkuSnapshot> getSkus(Collection<Long> skuIds) {
        Map<Long, SkuSnapshot> result = new HashMap<>();
        List<Long> ids = distinct(skuIds);
        if (ids.isEmpty()) {
            return result;
        }
        List<Sku> skus = skuMapper.selectBatchIds(ids);
        if (skus.isEmpty()) {
            return result;
        }
        Map<Long, Goods> goods = loadGoods(skus.stream().map(Sku::getGoodsId).toList());
        for (Sku s : skus) {
            Goods g = goods.get(s.getGoodsId());
            if (g == null) {
                // SPU 已删除时 SKU 视为不存在，购物车会把它显示为失效
                continue;
            }
            String pic = s.getPic() == null || s.getPic().isEmpty() ? g.getPic() : s.getPic();
            result.put(s.getId(), new SkuSnapshot(s.getId(), g.getId(), g.getCategoryId(), g.getName(), pic,
                    s.getSpecText(), s.getPrice(), s.getStock(), GoodsStatus.ON_SALE.name().equals(g.getStatus())));
        }
        return result;
    }

    @Override
    public Map<Long, GoodsBrief> getGoodsBriefs(Collection<Long> goodsIds) {
        Map<Long, GoodsBrief> result = new HashMap<>();
        loadGoods(distinct(goodsIds)).values().forEach(g -> result.put(g.getId(), new GoodsBrief(g.getId(), g.getName(),
                g.getPic(), g.getPrice(), g.getOriginalPrice(), GoodsConverter.displaySales(g),
                GoodsStatus.ON_SALE.name().equals(g.getStatus()))));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(Long skuId, int quantity) {
        checkQuantity(quantity);
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException("商品不存在或已删除");
        }
        if (skuMapper.deductStock(skuId, quantity) == 0) {
            Goods g = goodsMapper.selectById(sku.getGoodsId());
            String name = g == null ? "商品" : g.getName();
            throw new BusinessException(name + " 库存不足");
        }
        goodsMapper.deductStock(sku.getGoodsId(), quantity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreStock(Long skuId, int quantity) {
        checkQuantity(quantity);
        // 回补要包括已逻辑删除的 SKU（下单后商家删了规格，取消订单仍要把账还回去）
        Long goodsId = skuMapper.selectGoodsIdIncludingDeleted(skuId);
        if (goodsId == null) {
            return;
        }
        skuMapper.restoreStock(skuId, quantity);
        goodsMapper.restoreStock(goodsId, quantity);
    }

    @Override
    public Map<Long, ShippingInfo> getShipping(Collection<Long> skuIds) {
        Map<Long, ShippingInfo> result = new HashMap<>();
        List<Long> ids = distinct(skuIds);
        if (ids.isEmpty()) {
            return result;
        }
        List<Sku> skus = skuMapper.selectBatchIds(ids);
        if (skus.isEmpty()) {
            return result;
        }
        Map<Long, Goods> goods = goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                        .select(Goods::getId, Goods::getFreightTemplateId)
                        .in(Goods::getId, skus.stream().map(Sku::getGoodsId).distinct().toList()))
                .stream().collect(Collectors.toMap(Goods::getId, Function.identity()));
        for (Sku s : skus) {
            Goods g = goods.get(s.getGoodsId());
            if (g != null) {
                result.put(s.getId(), new ShippingInfo(s.getId(), g.getFreightTemplateId(), s.getWeight()));
            }
        }
        return result;
    }

    @Override
    public long countByFreightTemplate(Long templateId) {
        Long n = goodsMapper.selectCount(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getFreightTemplateId, templateId));
        return n == null ? 0 : n;
    }

    @Override
    public long countOnSale() {
        return goodsMapper.selectCount(new LambdaQueryWrapper<Goods>().eq(Goods::getStatus, GoodsStatus.ON_SALE.name()));
    }

    private Map<Long, Goods> loadGoods(List<Long> goodsIds) {
        if (goodsIds.isEmpty()) {
            return new HashMap<>();
        }
        return goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                        .select(Goods::getId, Goods::getCategoryId, Goods::getName, Goods::getPic, Goods::getPrice,
                                Goods::getOriginalPrice, Goods::getSales, Goods::getVirtualSales, Goods::getStatus)
                        .in(Goods::getId, distinct(goodsIds)))
                .stream().collect(Collectors.toMap(Goods::getId, Function.identity()));
    }

    private static List<Long> distinct(Collection<Long> ids) {
        return ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private static void checkQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("购买数量必须大于 0");
        }
    }
}
