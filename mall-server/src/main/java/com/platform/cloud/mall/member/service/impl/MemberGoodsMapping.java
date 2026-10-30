package com.platform.cloud.mall.member.service.impl;

import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.member.vo.MemberGoodsVO;

/**
 * 收藏 / 足迹共用的商品项映射。商品已被删除时仍返回该条记录（onSale=false），让用户能看到并移除它。
 */
final class MemberGoodsMapping {

    private MemberGoodsMapping() {
    }

    static MemberGoodsVO toVO(Long goodsId, GoodsApi.GoodsBrief brief) {
        MemberGoodsVO vo = new MemberGoodsVO();
        vo.setId(goodsId);
        if (brief == null) {
            vo.setOnSale(false);
            return vo;
        }
        vo.setName(brief.name());
        vo.setPic(brief.pic());
        vo.setPrice(brief.price());
        vo.setOriginalPrice(brief.originalPrice());
        vo.setSales(brief.sales());
        vo.setOnSale(brief.onSale());
        return vo;
    }
}
