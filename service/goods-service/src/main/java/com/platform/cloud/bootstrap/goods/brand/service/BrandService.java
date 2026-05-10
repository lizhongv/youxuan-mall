package com.platform.cloud.bootstrap.goods.brand.service;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.goods.brand.dto.BrandUpsertRequest;
import com.platform.cloud.bootstrap.goods.brand.vo.BrandVO;

public interface BrandService {

    Long create(BrandUpsertRequest req);

    void update(Long id, BrandUpsertRequest req);

    void delete(Long id);

    BrandVO getById(Long id);

    PageVO<BrandVO> page(PageQuery query);
}
