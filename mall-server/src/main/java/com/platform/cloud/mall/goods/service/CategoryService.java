package com.platform.cloud.mall.goods.service;

import com.platform.cloud.mall.goods.dto.CategoryRequest;
import com.platform.cloud.mall.goods.vo.CategoryVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 商品类目。类目最多两级，商品只能挂在二级类目下。
 */
public interface CategoryService {

    /** 管理端：全部类目树（含停用） */
    List<CategoryVO> adminTree();

    /** 商城端：启用的类目树（走缓存）；一级停用时其子类目也不展示 */
    List<CategoryVO> enabledTree();

    Long create(CategoryRequest req);

    void update(Long id, CategoryRequest req);

    /** 有子类目或商品时不能删 */
    void delete(Long id);

    /** 类目 ID 及其子类目 ID（一级类目查询商品时要包含子类目）；类目不存在时只返回自身，让查询结果为空 */
    List<Long> selfAndChildrenIds(Long categoryId);

    /**
     * 批量查父类目 ID：key 为类目 ID，value 为父 ID（一级类目为 0）。
     * 供营销领域判断「按类目适用」的优惠券：券上配置一级类目时要覆盖其下全部二级类目。
     */
    Map<Long, Long> parentIds(Collection<Long> categoryIds);

    /** 批量查类目名称（列表展示用） */
    Map<Long, String> names(Collection<Long> categoryIds);
}
