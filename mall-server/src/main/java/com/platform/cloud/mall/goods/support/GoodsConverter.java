package com.platform.cloud.mall.goods.support;

import com.platform.cloud.mall.goods.entity.Goods;
import com.platform.cloud.mall.goods.entity.Sku;
import com.platform.cloud.mall.goods.vo.GoodsItemVO;
import com.platform.cloud.mall.goods.vo.SkuVO;

import java.util.ArrayList;

/**
 * 商品实体到 VO 的转换。字段少且有展示口径（如销量含虚拟销量），手写比引入映射框架更直观。
 */
public final class GoodsConverter {

    private GoodsConverter() {
    }

    /** 前台展示销量 = 真实销量 + 虚拟销量（运营冷启动用的基础销量） */
    public static int displaySales(Goods g) {
        return nz(g.getSales()) + nz(g.getVirtualSales());
    }

    public static GoodsItemVO toItem(Goods g) {
        GoodsItemVO vo = new GoodsItemVO();
        vo.setId(g.getId());
        vo.setName(g.getName());
        vo.setSubtitle(g.getSubtitle());
        vo.setPic(g.getPic());
        vo.setPrice(g.getPrice());
        vo.setOriginalPrice(g.getOriginalPrice());
        vo.setSales(displaySales(g));
        vo.setIsNew(g.getIsNew());
        vo.setIsHot(g.getIsHot());
        return vo;
    }

    public static SkuVO toSkuVO(Sku s) {
        SkuVO vo = new SkuVO();
        vo.setId(s.getId());
        vo.setSpecValues(s.getSpecValues() == null ? new ArrayList<>() : s.getSpecValues());
        vo.setSpecText(s.getSpecText());
        vo.setCode(s.getCode());
        vo.setPic(s.getPic());
        vo.setPrice(s.getPrice());
        vo.setOriginalPrice(s.getOriginalPrice());
        vo.setStock(s.getStock());
        vo.setSales(s.getSales());
        return vo;
    }

    public static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
