package com.platform.cloud.mall.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.goods.dto.GoodsFlagsRequest;
import com.platform.cloud.mall.goods.dto.GoodsForm;
import com.platform.cloud.mall.goods.dto.GoodsForm.SkuForm;
import com.platform.cloud.mall.goods.dto.GoodsQuery;
import com.platform.cloud.mall.goods.dto.GoodsStatusRequest;
import com.platform.cloud.mall.goods.entity.Brand;
import com.platform.cloud.mall.goods.entity.Category;
import com.platform.cloud.mall.goods.entity.Goods;
import com.platform.cloud.mall.goods.entity.Sku;
import com.platform.cloud.mall.goods.entity.SpecItem;
import com.platform.cloud.mall.goods.mapper.BrandMapper;
import com.platform.cloud.mall.goods.mapper.CategoryMapper;
import com.platform.cloud.mall.goods.mapper.GoodsMapper;
import com.platform.cloud.mall.goods.mapper.SkuMapper;
import com.platform.cloud.mall.goods.service.BrandService;
import com.platform.cloud.mall.goods.service.CategoryService;
import com.platform.cloud.mall.goods.service.GoodsService;
import com.platform.cloud.mall.goods.support.GoodsCacheNames;
import com.platform.cloud.mall.goods.support.GoodsConverter;
import com.platform.cloud.mall.goods.support.GoodsSkuHelper;
import com.platform.cloud.mall.goods.support.GoodsStatus;
import com.platform.cloud.mall.goods.vo.GoodsAdminItemVO;
import com.platform.cloud.mall.goods.vo.GoodsDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端商品服务。
 */
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl implements GoodsService {

    /** 列表不查详情富文本，减少 IO */
    private static final String DETAIL_COLUMN = "detail";

    private final GoodsMapper goodsMapper;
    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;
    private final BrandMapper brandMapper;
    private final CategoryService categoryService;
    private final BrandService brandService;

    @Override
    public PageVO<GoodsAdminItemVO> page(GoodsQuery q) {
        LambdaQueryWrapper<Goods> w = new LambdaQueryWrapper<Goods>()
                .select(Goods.class, f -> !DETAIL_COLUMN.equals(f.getColumn()))
                .eq(q.getBrandId() != null, Goods::getBrandId, q.getBrandId())
                .eq(StringUtils.hasText(q.getStatus()), Goods::getStatus, q.getStatus())
                .orderByAsc(Goods::getSort).orderByDesc(Goods::getId);
        if (StringUtils.hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            w.and(x -> x.like(Goods::getName, kw).or().like(Goods::getKeywords, kw));
        }
        if (q.getCategoryId() != null) {
            w.in(Goods::getCategoryId, categoryService.selfAndChildrenIds(q.getCategoryId()));
        }
        Page<Goods> page = goodsMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w);
        Map<Long, String> categoryNames = categoryService.names(
                page.getRecords().stream().map(Goods::getCategoryId).toList());
        Map<Long, String> brandNames = brandService.names(
                page.getRecords().stream().map(Goods::getBrandId).toList());
        return PageVO.of(page, g -> toAdminItem(g, categoryNames, brandNames));
    }

    @Override
    public GoodsDetailVO detail(Long id) {
        Goods g = mustGet(id);
        GoodsDetailVO vo = new GoodsDetailVO();
        vo.setId(g.getId());
        vo.setCategoryId(g.getCategoryId());
        vo.setCategoryName(categoryService.names(List.of(g.getCategoryId())).get(g.getCategoryId()));
        vo.setBrandId(g.getBrandId());
        if (g.getBrandId() != null) {
            vo.setBrandName(brandService.names(List.of(g.getBrandId())).get(g.getBrandId()));
        }
        vo.setName(g.getName());
        vo.setSubtitle(g.getSubtitle());
        vo.setPic(g.getPic());
        vo.setAlbum(g.getAlbum() == null ? new ArrayList<>() : g.getAlbum());
        vo.setDetail(g.getDetail());
        vo.setUnit(g.getUnit());
        vo.setKeywords(g.getKeywords());
        vo.setVirtualSales(g.getVirtualSales());
        vo.setSort(g.getSort());
        vo.setStatus(g.getStatus());
        vo.setIsNew(g.getIsNew());
        vo.setIsHot(g.getIsHot());
        vo.setIsRecommend(g.getIsRecommend());
        vo.setSpecItems(g.getSpecItems() == null ? new ArrayList<>() : g.getSpecItems());
        vo.setSkus(listSkus(id).stream().map(GoodsConverter::toSkuVO).toList());
        vo.setPrice(g.getPrice());
        vo.setOriginalPrice(g.getOriginalPrice());
        vo.setStock(g.getStock());
        vo.setSales(g.getSales());
        vo.setCreatedAt(g.getCreatedAt());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = GoodsCacheNames.HOME, allEntries = true)
    public Long create(GoodsForm form) {
        List<SpecItem> specItems = prepare(form);
        Goods g = new Goods();
        g.setSales(0);
        apply(g, form, specItems);
        goodsMapper.insert(g);
        for (SkuForm sf : form.getSkus()) {
            skuMapper.insert(newSku(g.getId(), sf, specItems));
        }
        return g.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = GoodsCacheNames.HOME, allEntries = true)
    public void update(Long id, GoodsForm form) {
        Goods g = mustGet(id);
        List<SpecItem> specItems = prepare(form);
        apply(g, form, specItems);
        if (goodsMapper.updateById(g) == 0) {
            throw new BusinessException("商品已被修改，请刷新后重试");
        }
        syncSkus(id, form.getSkus(), specItems);
    }

    /**
     * 按 SKU id 对齐：有 id 的更新（必须属于本商品，防止篡改他人 SKU），无 id 的新增，表单里没有的逻辑删除。
     * 保留 SKU id 不变很重要：购物车、秒杀活动都按 SKU id 关联。
     */
    void syncSkus(Long goodsId, List<SkuForm> forms, List<SpecItem> specItems) {
        Map<Long, Sku> existing = listSkus(goodsId).stream()
                .collect(Collectors.toMap(Sku::getId, Function.identity()));
        Set<Long> kept = new HashSet<>();
        for (SkuForm sf : forms) {
            if (sf.getId() == null) {
                skuMapper.insert(newSku(goodsId, sf, specItems));
                continue;
            }
            Sku sku = existing.get(sf.getId());
            if (sku == null) {
                throw new BusinessException("SKU 不存在或不属于该商品");
            }
            if (!kept.add(sku.getId())) {
                throw new BusinessException("SKU 重复提交");
            }
            applySku(sku, sf, specItems);
            if (skuMapper.updateById(sku) == 0) {
                throw new BusinessException("SKU 库存已变化，请刷新后重试");
            }
        }
        List<Long> removed = existing.keySet().stream().filter(skuId -> !kept.contains(skuId)).toList();
        if (!removed.isEmpty()) {
            skuMapper.deleteBatchIds(removed);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = GoodsCacheNames.HOME, allEntries = true)
    public void delete(Long id) {
        Goods g = mustGet(id);
        if (GoodsStatus.ON_SALE.name().equals(g.getStatus())) {
            throw new BusinessException("请先下架商品再删除");
        }
        goodsMapper.deleteById(id);
        skuMapper.delete(new LambdaQueryWrapper<Sku>().eq(Sku::getGoodsId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = GoodsCacheNames.HOME, allEntries = true)
    public void updateStatus(GoodsStatusRequest req) {
        goodsMapper.update(null, new LambdaUpdateWrapper<Goods>()
                .set(Goods::getStatus, req.getStatus())
                .in(Goods::getId, req.getIds()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = GoodsCacheNames.HOME, allEntries = true)
    public void updateFlags(Long id, GoodsFlagsRequest req) {
        mustGet(id);
        LambdaUpdateWrapper<Goods> w = new LambdaUpdateWrapper<Goods>()
                .set(req.getIsNew() != null, Goods::getIsNew, req.getIsNew())
                .set(req.getIsHot() != null, Goods::getIsHot, req.getIsHot())
                .set(req.getIsRecommend() != null, Goods::getIsRecommend, req.getIsRecommend())
                .eq(Goods::getId, id);
        if (req.getIsNew() == null && req.getIsHot() == null && req.getIsRecommend() == null) {
            return;
        }
        goodsMapper.update(null, w);
    }

    /** 校验类目、品牌与规格，返回规整后的规格定义 */
    private List<SpecItem> prepare(GoodsForm form) {
        Category category = categoryMapper.selectById(form.getCategoryId());
        if (category == null) {
            throw new BusinessException("类目不存在");
        }
        if (category.getParentId() == null || category.getParentId() == Category.ROOT_ID) {
            throw new BusinessException("请选择二级类目");
        }
        if (form.getBrandId() != null) {
            Brand brand = brandMapper.selectById(form.getBrandId());
            if (brand == null) {
                throw new BusinessException("品牌不存在");
            }
        }
        List<SpecItem> specItems = GoodsSkuHelper.normalize(form.getSpecItems());
        for (SkuForm sf : form.getSkus()) {
            if (sf.getSpecValues() != null) {
                sf.setSpecValues(sf.getSpecValues().stream().map(v -> v == null ? "" : v.trim())
                        .collect(Collectors.toCollection(ArrayList::new)));
            }
        }
        GoodsSkuHelper.validate(specItems, form.getSkus());
        return specItems;
    }

    /** 写入 SPU 字段；价格、原价、库存由 SKU 汇总，不信任前端 */
    static void apply(Goods g, GoodsForm form, List<SpecItem> specItems) {
        GoodsSkuHelper.Summary summary = GoodsSkuHelper.summarize(form.getSkus());
        g.setCategoryId(form.getCategoryId());
        g.setBrandId(form.getBrandId());
        g.setName(form.getName().trim());
        g.setSubtitle(nullToEmpty(form.getSubtitle()));
        g.setPic(form.getPic());
        g.setAlbum(form.getAlbum() == null ? new ArrayList<>() : new ArrayList<>(form.getAlbum()));
        g.setDetail(nullToEmpty(form.getDetail()));
        g.setSpecItems(specItems);
        g.setPrice(summary.price());
        g.setOriginalPrice(summary.originalPrice());
        g.setStock(summary.stock());
        g.setVirtualSales(form.getVirtualSales() == null ? 0 : form.getVirtualSales());
        g.setUnit(StringUtils.hasText(form.getUnit()) ? form.getUnit() : "件");
        g.setKeywords(nullToEmpty(form.getKeywords()));
        g.setStatus(StringUtils.hasText(form.getStatus()) ? form.getStatus() : GoodsStatus.OFF_SALE.name());
        g.setIsNew(flag(form.getIsNew()));
        g.setIsHot(flag(form.getIsHot()));
        g.setIsRecommend(flag(form.getIsRecommend()));
        g.setSort(form.getSort() == null ? 0 : form.getSort());
    }

    private static Sku newSku(Long goodsId, SkuForm sf, List<SpecItem> specItems) {
        Sku sku = new Sku();
        sku.setGoodsId(goodsId);
        sku.setSales(0);
        applySku(sku, sf, specItems);
        return sku;
    }

    private static void applySku(Sku sku, SkuForm sf, List<SpecItem> specItems) {
        List<String> values = sf.getSpecValues() == null ? new ArrayList<>() : new ArrayList<>(sf.getSpecValues());
        sku.setSpecValues(values);
        sku.setSpecText(GoodsSkuHelper.specText(specItems, values));
        sku.setCode(nullToEmpty(sf.getCode()));
        sku.setPic(nullToEmpty(sf.getPic()));
        sku.setPrice(GoodsSkuHelper.money(sf.getPrice()));
        sku.setOriginalPrice(GoodsSkuHelper.money(sf.getOriginalPrice()));
        sku.setStock(sf.getStock());
    }

    private List<Sku> listSkus(Long goodsId) {
        return skuMapper.selectList(new LambdaQueryWrapper<Sku>().eq(Sku::getGoodsId, goodsId).orderByAsc(Sku::getId));
    }

    private Goods mustGet(Long id) {
        Goods g = goodsMapper.selectById(id);
        if (g == null) {
            throw new BusinessException("商品不存在");
        }
        return g;
    }

    private static GoodsAdminItemVO toAdminItem(Goods g, Map<Long, String> categoryNames, Map<Long, String> brandNames) {
        GoodsAdminItemVO vo = new GoodsAdminItemVO();
        vo.setId(g.getId());
        vo.setName(g.getName());
        vo.setPic(g.getPic());
        vo.setCategoryId(g.getCategoryId());
        vo.setCategoryName(categoryNames.get(g.getCategoryId()));
        vo.setBrandName(g.getBrandId() == null ? null : brandNames.get(g.getBrandId()));
        vo.setPrice(g.getPrice());
        vo.setOriginalPrice(g.getOriginalPrice());
        vo.setStock(g.getStock());
        vo.setSales(g.getSales());
        vo.setStatus(g.getStatus());
        vo.setIsNew(g.getIsNew());
        vo.setIsHot(g.getIsHot());
        vo.setIsRecommend(g.getIsRecommend());
        vo.setSort(g.getSort());
        vo.setCreatedAt(g.getCreatedAt());
        return vo;
    }

    private static int flag(Integer v) {
        return v != null && v == 1 ? 1 : 0;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
