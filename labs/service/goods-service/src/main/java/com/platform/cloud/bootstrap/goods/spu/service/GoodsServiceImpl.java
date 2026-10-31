package com.platform.cloud.bootstrap.goods.spu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.bootstrap.goods.spu.GoodsStatus;
import com.platform.cloud.bootstrap.goods.spu.converter.GoodsConverter;
import com.platform.cloud.bootstrap.goods.spu.dto.GoodsCreateRequest;
import com.platform.cloud.bootstrap.goods.spu.dto.GoodsQuery;
import com.platform.cloud.bootstrap.goods.spu.dto.GoodsUpdateRequest;
import com.platform.cloud.bootstrap.goods.spu.dto.SkuUpsertRequest;
import com.platform.cloud.bootstrap.goods.spu.entity.GoodsEntity;
import com.platform.cloud.bootstrap.goods.spu.entity.SkuEntity;
import com.platform.cloud.bootstrap.goods.spu.mapper.GoodsMapper;
import com.platform.cloud.bootstrap.goods.spu.mapper.SkuMapper;
import com.platform.cloud.bootstrap.goods.spu.vo.GoodsDetailVO;
import com.platform.cloud.bootstrap.goods.spu.vo.GoodsVO;
import com.platform.cloud.bootstrap.goods.spu.vo.SkuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl implements GoodsService {

    private static final String CACHE_DETAIL = "goods.spu.detail";

    private final GoodsMapper goodsMapper;
    private final SkuMapper skuMapper;
    private final GoodsConverter converter;

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_DETAIL, allEntries = true)
    public Long create(GoodsCreateRequest req) {
        if (goodsMapper.selectCount(new LambdaQueryWrapper<GoodsEntity>()
                .eq(GoodsEntity::getGoodsNo, req.getGoodsNo())) > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "商品编号已存在: " + req.getGoodsNo());
        }
        GoodsEntity entity = converter.toEntity(req);
        entity.setStatus(GoodsStatus.DRAFT);
        goodsMapper.insert(entity);

        for (SkuUpsertRequest skuReq : req.getSkus()) {
            checkSkuNoUnique(skuReq.getSkuNo(), null);
            SkuEntity sku = converter.toSkuEntity(skuReq);
            sku.setGoodsId(entity.getId());
            skuMapper.insert(sku);
        }
        recomputePriceRange(entity.getId());
        log.info("Goods created id={} goodsNo={} skus={}", entity.getId(), entity.getGoodsNo(), req.getSkus().size());
        return entity.getId();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_DETAIL, key = "#id")
    public void update(Long id, GoodsUpdateRequest req) {
        GoodsEntity entity = mustGet(id);
        converter.updateEntity(req, entity);
        goodsMapper.updateById(entity);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_DETAIL, key = "#id")
    public void delete(Long id) {
        GoodsEntity entity = mustGet(id);
        if (entity.getStatus() == GoodsStatus.ON_SALE) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "上架中商品不能删除，请先下架");
        }
        // 级联软删 SKU
        List<SkuEntity> skus = listSkus(id);
        for (SkuEntity sku : skus) {
            skuMapper.deleteById(sku.getId());
        }
        goodsMapper.deleteById(id);
    }

    @Override
    @Cacheable(cacheNames = CACHE_DETAIL, key = "#id")
    public GoodsDetailVO detail(Long id) {
        GoodsEntity entity = mustGet(id);
        GoodsDetailVO vo = converter.toDetailVO(entity);
        vo.setSkus(converter.toSkuVOList(listSkus(id)));
        return vo;
    }

    @Override
    public PageVO<GoodsVO> page(GoodsQuery query) {
        LambdaQueryWrapper<GoodsEntity> q = new LambdaQueryWrapper<>();
        if (query.getCategoryId() != null) q.eq(GoodsEntity::getCategoryId, query.getCategoryId());
        if (query.getBrandId() != null) q.eq(GoodsEntity::getBrandId, query.getBrandId());
        if (query.getStatus() != null) q.eq(GoodsEntity::getStatus, query.getStatus());
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            q.and(w -> w.like(GoodsEntity::getName, query.getKeyword())
                    .or().like(GoodsEntity::getGoodsNo, query.getKeyword()));
        }
        q.orderByAsc(GoodsEntity::getSortOrder).orderByDesc(GoodsEntity::getId);
        Page<GoodsEntity> page = goodsMapper.selectPage(new Page<>(query.getPage(), query.getSize()), q);
        return PageVO.of(page, converter::toVO);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_DETAIL, key = "#id")
    public void publish(Long id) {
        transition(id, GoodsStatus.ON_SALE);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_DETAIL, key = "#id")
    public void unpublish(Long id) {
        transition(id, GoodsStatus.OFF_SALE);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_DETAIL, key = "#goodsId")
    public Long addSku(Long goodsId, SkuUpsertRequest req) {
        mustGet(goodsId);
        checkSkuNoUnique(req.getSkuNo(), null);
        SkuEntity sku = converter.toSkuEntity(req);
        sku.setGoodsId(goodsId);
        skuMapper.insert(sku);
        recomputePriceRange(goodsId);
        return sku.getId();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_DETAIL, key = "#goodsId")
    public void updateSku(Long goodsId, Long skuId, SkuUpsertRequest req) {
        SkuEntity sku = mustGetSku(goodsId, skuId);
        if (req.getSkuNo() != null && !req.getSkuNo().equals(sku.getSkuNo())) {
            checkSkuNoUnique(req.getSkuNo(), skuId);
        }
        converter.updateSkuEntity(req, sku);
        skuMapper.updateById(sku);
        recomputePriceRange(goodsId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_DETAIL, key = "#goodsId")
    public void deleteSku(Long goodsId, Long skuId) {
        SkuEntity sku = mustGetSku(goodsId, skuId);
        long remaining = skuMapper.selectCount(new LambdaQueryWrapper<SkuEntity>()
                .eq(SkuEntity::getGoodsId, goodsId));
        if (remaining <= 1) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "SPU 至少保留 1 个 SKU");
        }
        skuMapper.deleteById(sku.getId());
        recomputePriceRange(goodsId);
    }

    @Override
    public SkuVO getSku(Long goodsId, Long skuId) {
        return converter.toSkuVO(mustGetSku(goodsId, skuId));
    }

    private void transition(Long id, GoodsStatus to) {
        GoodsEntity entity = mustGet(id);
        if (entity.getStatus() == to) return;
        if (!entity.getStatus().canTransitionTo(to)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "状态不允许从 " + entity.getStatus() + " → " + to);
        }
        if (to == GoodsStatus.ON_SALE && listSkus(id).isEmpty()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "上架前必须配置至少 1 个 SKU");
        }
        entity.setStatus(to);
        goodsMapper.updateById(entity);
        log.info("Goods {} transition → {}", id, to);
    }

    private void recomputePriceRange(Long goodsId) {
        List<SkuEntity> skus = listSkus(goodsId);
        if (skus.isEmpty()) return;
        BigDecimal min = skus.stream().map(SkuEntity::getPrice).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal max = skus.stream().map(SkuEntity::getPrice).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        GoodsEntity goods = goodsMapper.selectById(goodsId);
        if (goods == null) return;
        goods.setMinPrice(min);
        goods.setMaxPrice(max);
        goodsMapper.updateById(goods);
    }

    private GoodsEntity mustGet(Long id) {
        GoodsEntity e = goodsMapper.selectById(id);
        if (e == null) throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在: " + id);
        return e;
    }

    private SkuEntity mustGetSku(Long goodsId, Long skuId) {
        SkuEntity sku = skuMapper.selectById(skuId);
        if (sku == null || !goodsId.equals(sku.getGoodsId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "SKU 不存在: " + skuId);
        }
        return sku;
    }

    private List<SkuEntity> listSkus(Long goodsId) {
        return skuMapper.selectList(new LambdaQueryWrapper<SkuEntity>()
                .eq(SkuEntity::getGoodsId, goodsId)
                .orderByAsc(SkuEntity::getId));
    }

    private void checkSkuNoUnique(String skuNo, Long excludeId) {
        LambdaQueryWrapper<SkuEntity> q = new LambdaQueryWrapper<SkuEntity>()
                .eq(SkuEntity::getSkuNo, skuNo);
        if (excludeId != null) q.ne(SkuEntity::getId, excludeId);
        if (skuMapper.selectCount(q) > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "SKU 编号已存在: " + skuNo);
        }
    }
}
