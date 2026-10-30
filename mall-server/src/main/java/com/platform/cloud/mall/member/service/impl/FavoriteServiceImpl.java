package com.platform.cloud.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.member.entity.MbrFavorite;
import com.platform.cloud.mall.member.mapper.MbrFavoriteMapper;
import com.platform.cloud.mall.member.service.FavoriteService;
import com.platform.cloud.mall.member.vo.MemberGoodsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 商品收藏。商品信息每次从商品领域实时取，价格、上下架状态与详情页一致。
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    /** 收藏上限，防止脚本无限写入 */
    static final int MAX_FAVORITES = 500;

    private final MbrFavoriteMapper favoriteMapper;
    private final GoodsApi goodsApi;

    @Override
    public PageVO<MemberGoodsVO> page(Long memberId, PageQuery q) {
        Page<MbrFavorite> page = favoriteMapper.selectPage(new Page<>(q.getPage(), q.getSize()),
                new LambdaQueryWrapper<MbrFavorite>()
                        .eq(MbrFavorite::getMemberId, memberId)
                        .orderByDesc(MbrFavorite::getCreatedAt));
        Map<Long, GoodsApi.GoodsBrief> briefs = page.getRecords().isEmpty()
                ? Map.of()
                : goodsApi.getGoodsBriefs(page.getRecords().stream().map(MbrFavorite::getGoodsId).toList());
        return PageVO.of(page, f -> {
            MemberGoodsVO vo = MemberGoodsMapping.toVO(f.getGoodsId(), briefs.get(f.getGoodsId()));
            vo.setFavoritedAt(f.getCreatedAt());
            return vo;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Long memberId, Long goodsId) {
        if (exists(memberId, goodsId)) {
            return;
        }
        if (goodsApi.getGoodsBriefs(List.of(goodsId)).get(goodsId) == null) {
            throw new BusinessException("商品不存在");
        }
        if (count(memberId) >= MAX_FAVORITES) {
            throw new BusinessException("收藏已达上限 " + MAX_FAVORITES + " 个，请先清理");
        }
        MbrFavorite f = new MbrFavorite();
        f.setMemberId(memberId);
        f.setGoodsId(goodsId);
        favoriteMapper.insert(f);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long memberId, Long goodsId) {
        favoriteMapper.delete(new LambdaQueryWrapper<MbrFavorite>()
                .eq(MbrFavorite::getMemberId, memberId)
                .eq(MbrFavorite::getGoodsId, goodsId));
    }

    @Override
    public long count(Long memberId) {
        Long n = favoriteMapper.selectCount(new LambdaQueryWrapper<MbrFavorite>().eq(MbrFavorite::getMemberId, memberId));
        return n == null ? 0 : n;
    }

    private boolean exists(Long memberId, Long goodsId) {
        Long n = favoriteMapper.selectCount(new LambdaQueryWrapper<MbrFavorite>()
                .eq(MbrFavorite::getMemberId, memberId)
                .eq(MbrFavorite::getGoodsId, goodsId));
        return n != null && n > 0;
    }
}
