package com.platform.cloud.bootstrap.goods.spu.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.goods.spu.dto.GoodsCreateRequest;
import com.platform.cloud.bootstrap.goods.spu.dto.GoodsQuery;
import com.platform.cloud.bootstrap.goods.spu.dto.GoodsUpdateRequest;
import com.platform.cloud.bootstrap.goods.spu.dto.SkuUpsertRequest;
import com.platform.cloud.bootstrap.goods.spu.vo.GoodsDetailVO;
import com.platform.cloud.bootstrap.goods.spu.vo.GoodsVO;
import com.platform.cloud.bootstrap.goods.spu.vo.SkuVO;

public interface GoodsService {

    Long create(GoodsCreateRequest req);

    void update(Long id, GoodsUpdateRequest req);

    void delete(Long id);

    GoodsDetailVO detail(Long id);

    PageVO<GoodsVO> page(GoodsQuery query);

    /** 状态机：发布上架（DRAFT/OFF_SALE → ON_SALE） */
    void publish(Long id);

    /** 状态机：下架（ON_SALE → OFF_SALE） */
    void unpublish(Long id);

    Long addSku(Long goodsId, SkuUpsertRequest req);

    void updateSku(Long goodsId, Long skuId, SkuUpsertRequest req);

    void deleteSku(Long goodsId, Long skuId);

    SkuVO getSku(Long goodsId, Long skuId);
}
