package com.platform.cloud.mall.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.platform.cloud.mall.goods.entity.Goods;
import com.platform.cloud.mall.goods.mapper.GoodsMapper;
import com.platform.cloud.mall.goods.service.CategoryService;
import com.platform.cloud.mall.goods.support.GoodsCacheNames;
import com.platform.cloud.mall.goods.support.GoodsConverter;
import com.platform.cloud.mall.goods.support.GoodsStatus;
import com.platform.cloud.mall.goods.vo.CategoryVO;
import com.platform.cloud.mall.goods.vo.GoodsItemVO;
import com.platform.cloud.mall.goods.vo.HomeVO;
import com.platform.cloud.mall.marketing.service.BannerService;
import com.platform.cloud.mall.marketing.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页可缓存部分的加载器。单独成 Bean 是为了让 {@code @Cacheable} 经过代理生效（同类内部调用不走缓存）。
 */
@Component
@RequiredArgsConstructor
public class HomeContentLoader {

    /** 首页每个商品楼层最多 10 个，与接口约定一致 */
    static final int GOODS_PER_FLOOR = 10;
    /** 首页公告条数：滚动公告条展示最近几条即可 */
    static final int NOTICE_LIMIT = 5;
    static final String HOME_POSITION = "HOME";

    private final GoodsMapper goodsMapper;
    private final CategoryService categoryService;
    private final BannerService bannerService;
    private final NoticeService noticeService;

    @Cacheable(cacheNames = GoodsCacheNames.HOME, key = "'base'")
    public HomeVO load() {
        HomeVO vo = new HomeVO();
        vo.setBanners(new ArrayList<>(bannerService.listEnabled(HOME_POSITION)));
        vo.setNotices(new ArrayList<>(noticeService.listLatest(NOTICE_LIMIT)));
        List<CategoryVO> roots = new ArrayList<>();
        for (CategoryVO root : categoryService.enabledTree()) {
            // 首页只要一级类目，复制一份去掉 children，避免改到类目树缓存里的对象
            CategoryVO copy = new CategoryVO();
            copy.setId(root.getId());
            copy.setParentId(root.getParentId());
            copy.setName(root.getName());
            copy.setIcon(root.getIcon());
            copy.setSort(root.getSort());
            copy.setStatus(root.getStatus());
            roots.add(copy);
        }
        vo.setCategories(roots);
        vo.setNewGoods(floor(Goods::getIsNew));
        vo.setHotGoods(floor(Goods::getIsHot));
        vo.setRecommendGoods(floor(Goods::getIsRecommend));
        return vo;
    }

    private List<GoodsItemVO> floor(SFunction<Goods, Integer> flag) {
        List<Goods> list = goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .select(Goods::getId, Goods::getName, Goods::getSubtitle, Goods::getPic, Goods::getPrice,
                        Goods::getOriginalPrice, Goods::getSales, Goods::getVirtualSales, Goods::getIsNew,
                        Goods::getIsHot)
                .eq(Goods::getStatus, GoodsStatus.ON_SALE.name())
                .eq(flag, 1)
                .orderByAsc(Goods::getSort).orderByDesc(Goods::getId)
                .last("LIMIT " + GOODS_PER_FLOOR));
        List<GoodsItemVO> result = new ArrayList<>();
        list.forEach(g -> result.add(GoodsConverter.toItem(g)));
        return result;
    }
}
