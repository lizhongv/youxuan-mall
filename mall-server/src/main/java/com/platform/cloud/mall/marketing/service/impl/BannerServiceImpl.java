package com.platform.cloud.mall.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.goods.support.GoodsCacheNames;
import com.platform.cloud.mall.marketing.dto.BannerQuery;
import com.platform.cloud.mall.marketing.dto.BannerRequest;
import com.platform.cloud.mall.marketing.entity.Banner;
import com.platform.cloud.mall.marketing.mapper.BannerMapper;
import com.platform.cloud.mall.marketing.service.BannerService;
import com.platform.cloud.mall.marketing.vo.BannerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 轮播广告。写操作使首页缓存失效。
 */
@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private static final String DEFAULT_POSITION = "HOME";
    private static final String LINK_NONE = "NONE";

    private final BannerMapper bannerMapper;

    @Override
    public PageVO<BannerVO> page(BannerQuery q) {
        LambdaQueryWrapper<Banner> w = new LambdaQueryWrapper<Banner>()
                .like(StringUtils.hasText(q.getKeyword()), Banner::getTitle, q.getKeyword())
                .eq(StringUtils.hasText(q.getPosition()), Banner::getPosition, q.getPosition())
                .eq(q.getStatus() != null, Banner::getStatus, q.getStatus())
                .orderByAsc(Banner::getPosition).orderByAsc(Banner::getSort).orderByDesc(Banner::getId);
        return PageVO.of(bannerMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w), BannerServiceImpl::toVO);
    }

    @Override
    public BannerVO get(Long id) {
        return toVO(mustGet(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = GoodsCacheNames.HOME, allEntries = true)
    public Long create(BannerRequest req) {
        Banner b = new Banner();
        apply(b, req);
        bannerMapper.insert(b);
        return b.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = GoodsCacheNames.HOME, allEntries = true)
    public void update(Long id, BannerRequest req) {
        Banner b = mustGet(id);
        apply(b, req);
        if (bannerMapper.updateById(b) == 0) {
            throw new BusinessException("数据已被修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = GoodsCacheNames.HOME, allEntries = true)
    public void delete(Long id) {
        mustGet(id);
        bannerMapper.deleteById(id);
    }

    @Override
    public List<BannerVO> listEnabled(String position) {
        List<BannerVO> result = new ArrayList<>();
        bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                        .eq(Banner::getPosition, position)
                        .eq(Banner::getStatus, 1)
                        .orderByAsc(Banner::getSort).orderByDesc(Banner::getId))
                .forEach(b -> result.add(toVO(b)));
        return result;
    }

    private Banner mustGet(Long id) {
        Banner b = bannerMapper.selectById(id);
        if (b == null) {
            throw new BusinessException("轮播不存在");
        }
        return b;
    }

    private static void apply(Banner b, BannerRequest req) {
        String linkType = StringUtils.hasText(req.getLinkType()) ? req.getLinkType() : LINK_NONE;
        String linkValue = req.getLinkValue() == null ? "" : req.getLinkValue().trim();
        if (!LINK_NONE.equals(linkType) && linkValue.isEmpty()) {
            throw new BusinessException("请填写跳转目标");
        }
        if (("GOODS".equals(linkType) || "CATEGORY".equals(linkType)) && !linkValue.matches("\\d{1,19}")) {
            throw new BusinessException("跳转商品或类目时请填写 ID");
        }
        b.setTitle(req.getTitle().trim());
        b.setImage(req.getImage());
        b.setLinkType(linkType);
        b.setLinkValue(LINK_NONE.equals(linkType) ? "" : linkValue);
        b.setPosition(StringUtils.hasText(req.getPosition()) ? req.getPosition() : DEFAULT_POSITION);
        b.setSort(req.getSort() == null ? 0 : req.getSort());
        b.setStatus(req.getStatus() == null ? 1 : req.getStatus());
    }

    static BannerVO toVO(Banner b) {
        BannerVO vo = new BannerVO();
        vo.setId(b.getId());
        vo.setTitle(b.getTitle());
        vo.setImage(b.getImage());
        vo.setLinkType(b.getLinkType());
        vo.setLinkValue(b.getLinkValue());
        vo.setPosition(b.getPosition());
        vo.setSort(b.getSort());
        vo.setStatus(b.getStatus());
        return vo;
    }
}
