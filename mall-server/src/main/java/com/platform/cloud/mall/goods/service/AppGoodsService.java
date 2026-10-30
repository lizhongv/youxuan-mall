package com.platform.cloud.mall.goods.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.goods.dto.AppGoodsQuery;
import com.platform.cloud.mall.goods.vo.AppGoodsDetailVO;
import com.platform.cloud.mall.goods.vo.GoodsItemVO;
import com.platform.cloud.mall.goods.vo.HomeVO;
import com.platform.cloud.mall.goods.vo.MallConfigVO;

/**
 * 商城端公开浏览：首页、商品搜索、商品详情、公开参数。
 */
public interface AppGoodsService {

    HomeVO home();

    /** 只返回在售商品 */
    PageVO<GoodsItemVO> page(AppGoodsQuery q);

    /**
     * 商品详情；下架商品返回业务提示「商品已下架」。
     *
     * @param memberId 当前会员 ID，未登录为 null；非空时返回 favorited 并记录足迹
     */
    AppGoodsDetailVO detail(Long id, Long memberId);

    MallConfigVO mallConfig();
}
