package com.platform.cloud.mall.goods.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.goods.dto.BrandQuery;
import com.platform.cloud.mall.goods.dto.BrandRequest;
import com.platform.cloud.mall.goods.vo.BrandVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 品牌。
 */
public interface BrandService {

    PageVO<BrandVO> page(BrandQuery q);

    /** 全部启用品牌（商品编辑页下拉） */
    List<BrandVO> listEnabled();

    BrandVO get(Long id);

    Long create(BrandRequest req);

    void update(Long id, BrandRequest req);

    /** 仍有商品使用的品牌不能删，避免商品详情出现悬空品牌 */
    void delete(Long id);

    /** 批量查品牌名称 */
    Map<Long, String> names(Collection<Long> brandIds);
}
