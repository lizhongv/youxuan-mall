package com.platform.cloud.mall.goods.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.goods.dto.GoodsFlagsRequest;
import com.platform.cloud.mall.goods.dto.GoodsForm;
import com.platform.cloud.mall.goods.dto.GoodsQuery;
import com.platform.cloud.mall.goods.dto.GoodsStatusRequest;
import com.platform.cloud.mall.goods.vo.GoodsAdminItemVO;
import com.platform.cloud.mall.goods.vo.GoodsDetailVO;

/**
 * 管理端商品维护：SPU + SKU 一起保存。
 */
public interface GoodsService {

    PageVO<GoodsAdminItemVO> page(GoodsQuery q);

    GoodsDetailVO detail(Long id);

    Long create(GoodsForm form);

    /** 按 SKU id 更新已有 SKU、新增无 id 的 SKU、删除表单中不再出现的 SKU */
    void update(Long id, GoodsForm form);

    /** 仅允许删除已下架商品，避免误删正在售卖的商品 */
    void delete(Long id);

    void updateStatus(GoodsStatusRequest req);

    void updateFlags(Long id, GoodsFlagsRequest req);
}
