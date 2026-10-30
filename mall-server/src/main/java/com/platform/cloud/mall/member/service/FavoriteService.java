package com.platform.cloud.mall.member.service;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.member.vo.MemberGoodsVO;

/**
 * 商品收藏。
 */
public interface FavoriteService {

    PageVO<MemberGoodsVO> page(Long memberId, PageQuery query);

    /** 收藏；重复收藏视为成功 */
    void add(Long memberId, Long goodsId);

    void remove(Long memberId, Long goodsId);

    long count(Long memberId);
}
