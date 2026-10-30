package com.platform.cloud.mall.member.service;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.member.vo.MemberGoodsVO;

/**
 * 浏览足迹。写入由商品详情页通过 MemberApi.recordBrowse 完成。
 */
public interface BrowseHistoryService {

    PageVO<MemberGoodsVO> page(Long memberId, PageQuery query);

    void clear(Long memberId);
}
