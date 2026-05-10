package com.platform.cloud.bootstrap.goods.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.bootstrap.goods.category.converter.CategoryConverter;
import com.platform.cloud.bootstrap.goods.category.dto.CategoryUpsertRequest;
import com.platform.cloud.bootstrap.goods.category.entity.CategoryEntity;
import com.platform.cloud.bootstrap.goods.category.mapper.CategoryMapper;
import com.platform.cloud.bootstrap.goods.category.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String CACHE_ITEM = "goods.category.item";
    private static final String CACHE_TREE = "goods.category.tree";

    private final CategoryMapper mapper;
    private final CategoryConverter converter;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CACHE_ITEM, CACHE_TREE}, allEntries = true)
    public Long create(CategoryUpsertRequest req) {
        validateParent(req.getParentId());
        CategoryEntity entity = converter.toEntity(req);
        mapper.insert(entity);
        log.info("Category created id={} name={}", entity.getId(), entity.getName());
        return entity.getId();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CACHE_ITEM, CACHE_TREE}, allEntries = true)
    public void update(Long id, CategoryUpsertRequest req) {
        CategoryEntity entity = mustGet(id);
        if (req.getParentId() != null && !req.getParentId().equals(entity.getParentId())) {
            if (req.getParentId().equals(id)) {
                throw new BusinessException(ResultCode.PARAM_INVALID, "父分类不能是自己");
            }
            validateParent(req.getParentId());
        }
        converter.updateEntity(req, entity);
        mapper.updateById(entity);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CACHE_ITEM, CACHE_TREE}, allEntries = true)
    public void delete(Long id) {
        mustGet(id);
        long childCount = mapper.selectCount(new LambdaQueryWrapper<CategoryEntity>()
                .eq(CategoryEntity::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "存在子分类，无法删除");
        }
        mapper.deleteById(id);
    }

    @Override
    @Cacheable(cacheNames = CACHE_ITEM, key = "#id")
    public CategoryVO getById(Long id) {
        return converter.toVO(mustGet(id));
    }

    @Override
    public PageVO<CategoryVO> page(PageQuery query) {
        LambdaQueryWrapper<CategoryEntity> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            q.like(CategoryEntity::getName, query.getKeyword());
        }
        q.orderByAsc(CategoryEntity::getSortOrder).orderByDesc(CategoryEntity::getId);
        Page<CategoryEntity> page = mapper.selectPage(new Page<>(query.getPage(), query.getSize()), q);
        return PageVO.of(page, converter::toVO);
    }

    @Override
    @Cacheable(cacheNames = CACHE_TREE)
    public List<CategoryVO> tree() {
        List<CategoryEntity> all = mapper.selectList(new LambdaQueryWrapper<CategoryEntity>()
                .eq(CategoryEntity::getVisible, 1)
                .orderByAsc(CategoryEntity::getSortOrder));
        Map<Long, CategoryVO> byId = new LinkedHashMap<>();
        for (CategoryEntity e : all) {
            byId.put(e.getId(), converter.toVO(e));
        }
        List<CategoryVO> roots = new ArrayList<>();
        for (CategoryVO vo : byId.values()) {
            if (vo.getParentId() == null || vo.getParentId() == 0L) {
                roots.add(vo);
            } else {
                CategoryVO parent = byId.get(vo.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    roots.add(vo); // 父被删除/不可见时，挂到顶层避免丢失
                }
            }
        }
        return roots;
    }

    private CategoryEntity mustGet(Long id) {
        CategoryEntity e = mapper.selectById(id);
        if (e == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在: " + id);
        }
        return e;
    }

    private void validateParent(Long parentId) {
        if (parentId == null || parentId == 0L) return;
        if (mapper.selectById(parentId) == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "父分类不存在: " + parentId);
        }
    }
}
