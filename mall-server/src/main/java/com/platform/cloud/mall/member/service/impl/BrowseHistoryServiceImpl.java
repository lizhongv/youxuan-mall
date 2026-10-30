package com.platform.cloud.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.member.entity.MbrBrowseHistory;
import com.platform.cloud.mall.member.mapper.MbrBrowseHistoryMapper;
import com.platform.cloud.mall.member.service.BrowseHistoryService;
import com.platform.cloud.mall.member.vo.MemberGoodsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 浏览足迹查询与清空。
 */
@Service
@RequiredArgsConstructor
public class BrowseHistoryServiceImpl implements BrowseHistoryService {

    private final MbrBrowseHistoryMapper historyMapper;
    private final GoodsApi goodsApi;

    @Override
    public PageVO<MemberGoodsVO> page(Long memberId, PageQuery q) {
        Page<MbrBrowseHistory> page = historyMapper.selectPage(new Page<>(q.getPage(), q.getSize()),
                new LambdaQueryWrapper<MbrBrowseHistory>()
                        .eq(MbrBrowseHistory::getMemberId, memberId)
                        .orderByDesc(MbrBrowseHistory::getUpdatedAt));
        Map<Long, GoodsApi.GoodsBrief> briefs = page.getRecords().isEmpty()
                ? Map.of()
                : goodsApi.getGoodsBriefs(page.getRecords().stream().map(MbrBrowseHistory::getGoodsId).toList());
        return PageVO.of(page, h -> {
            MemberGoodsVO vo = MemberGoodsMapping.toVO(h.getGoodsId(), briefs.get(h.getGoodsId()));
            vo.setBrowsedAt(h.getUpdatedAt());
            return vo;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clear(Long memberId) {
        historyMapper.delete(new LambdaQueryWrapper<MbrBrowseHistory>().eq(MbrBrowseHistory::getMemberId, memberId));
    }
}
