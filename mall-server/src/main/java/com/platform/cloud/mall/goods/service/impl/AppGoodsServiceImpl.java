package com.platform.cloud.mall.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.goods.dto.AppGoodsQuery;
import com.platform.cloud.mall.goods.entity.Goods;
import com.platform.cloud.mall.goods.entity.Sku;
import com.platform.cloud.mall.goods.mapper.GoodsMapper;
import com.platform.cloud.mall.goods.mapper.SkuMapper;
import com.platform.cloud.mall.goods.service.AppGoodsService;
import com.platform.cloud.mall.goods.service.BrandService;
import com.platform.cloud.mall.goods.service.CategoryService;
import com.platform.cloud.mall.goods.support.GoodsConverter;
import com.platform.cloud.mall.goods.support.GoodsStatus;
import com.platform.cloud.mall.goods.vo.AppGoodsDetailVO;
import com.platform.cloud.mall.goods.vo.GoodsItemVO;
import com.platform.cloud.mall.goods.vo.HomeVO;
import com.platform.cloud.mall.goods.vo.MallConfigVO;
import com.platform.cloud.mall.marketing.service.SeckillService;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.system.api.ConfigApi;
import com.platform.cloud.mall.system.api.ConfigKeys;
import com.platform.cloud.mall.trade.api.TradeApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商城端公开浏览。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppGoodsServiceImpl implements AppGoodsService {

    /** 首页秒杀楼层最多 4 个，与接口约定一致 */
    static final int HOME_SECKILL_LIMIT = 4;
    private static final String DEFAULT_SORT = "DEFAULT";

    private final GoodsMapper goodsMapper;
    private final SkuMapper skuMapper;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final HomeContentLoader homeContentLoader;
    private final SeckillService seckillService;
    private final MemberApi memberApi;
    private final TradeApi tradeApi;
    private final ConfigApi configApi;

    @Override
    public HomeVO home() {
        HomeVO vo = homeContentLoader.load().copy();
        vo.setSeckills(new ArrayList<>(seckillService.listForHome(HOME_SECKILL_LIMIT)));
        return vo;
    }

    @Override
    public PageVO<GoodsItemVO> page(AppGoodsQuery q) {
        List<Long> categoryIds = q.getCategoryId() == null ? null : categoryService.selfAndChildrenIds(q.getCategoryId());
        String keyword = StringUtils.hasText(q.getKeyword()) ? q.getKeyword().trim() : null;
        String sort = StringUtils.hasText(q.getSort()) ? q.getSort() : DEFAULT_SORT;
        IPage<Goods> page = goodsMapper.pageOnSale(new Page<>(q.getPage(), q.getSize()), keyword, categoryIds,
                q.getBrandId(), sort);
        return PageVO.of(page, GoodsConverter::toItem);
    }

    @Override
    public AppGoodsDetailVO detail(Long id, Long memberId) {
        Goods g = goodsMapper.selectById(id);
        if (g == null) {
            throw new BusinessException("商品不存在");
        }
        if (!GoodsStatus.ON_SALE.name().equals(g.getStatus())) {
            throw new BusinessException("商品已下架");
        }
        AppGoodsDetailVO vo = new AppGoodsDetailVO();
        vo.setId(g.getId());
        vo.setCategoryId(g.getCategoryId());
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
        vo.setStatus(g.getStatus());
        vo.setIsNew(g.getIsNew());
        vo.setIsHot(g.getIsHot());
        vo.setIsRecommend(g.getIsRecommend());
        vo.setPrice(g.getPrice());
        vo.setOriginalPrice(g.getOriginalPrice());
        vo.setStock(g.getStock());
        vo.setSales(GoodsConverter.displaySales(g));
        vo.setSpecItems(g.getSpecItems() == null ? new ArrayList<>() : g.getSpecItems());
        vo.setSkus(skuMapper.selectList(new LambdaQueryWrapper<Sku>().eq(Sku::getGoodsId, id).orderByAsc(Sku::getId))
                .stream().map(GoodsConverter::toSkuVO).toList());

        TradeApi.ReviewSummary summary = tradeApi.reviewSummary(id);
        if (summary != null) {
            vo.setReviewCount(summary.total());
            vo.setGoodRate(summary.goodRate());
            vo.setLatestReviews(summary.latest() == null ? List.of() : summary.latest());
        } else {
            vo.setReviewCount(0L);
            vo.setGoodRate(100);
            vo.setLatestReviews(List.of());
        }

        vo.setSeckill(seckillService.findOngoing(id));

        if (memberId != null) {
            vo.setFavorited(memberApi.isFavorited(memberId, id));
            try {
                memberApi.recordBrowse(memberId, id);
            } catch (RuntimeException e) {
                // 足迹只是辅助功能，记录失败不能影响用户看商品
                log.warn("记录浏览足迹失败 memberId={} goodsId={}: {}", memberId, id, e.getMessage());
            }
        }
        return vo;
    }

    @Override
    public MallConfigVO mallConfig() {
        MallConfigVO vo = new MallConfigVO();
        vo.setMallName(configApi.getString(ConfigKeys.MALL_NAME, ""));
        vo.setServicePhone(configApi.getString(ConfigKeys.MALL_SERVICE_PHONE, ""));
        vo.setFreightAmount(configApi.getDecimal(ConfigKeys.FREIGHT_AMOUNT, BigDecimal.ZERO));
        vo.setFreightFreeThreshold(configApi.getDecimal(ConfigKeys.FREIGHT_FREE_THRESHOLD, BigDecimal.ZERO));
        return vo;
    }
}
