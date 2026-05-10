package com.platform.cloud.bootstrap.goods.category.service;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.goods.category.dto.CategoryUpsertRequest;
import com.platform.cloud.bootstrap.goods.category.vo.CategoryVO;

import java.util.List;

public interface CategoryService {

    Long create(CategoryUpsertRequest req);

    void update(Long id, CategoryUpsertRequest req);

    void delete(Long id);

    CategoryVO getById(Long id);

    PageVO<CategoryVO> page(PageQuery query);

    List<CategoryVO> tree();
}
