package com.platform.cloud.mall.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.goods.dto.CategoryRequest;
import com.platform.cloud.mall.goods.entity.Category;
import com.platform.cloud.mall.goods.entity.Goods;
import com.platform.cloud.mall.goods.mapper.CategoryMapper;
import com.platform.cloud.mall.goods.mapper.GoodsMapper;
import com.platform.cloud.mall.goods.service.CategoryService;
import com.platform.cloud.mall.goods.support.GoodsCacheNames;
import com.platform.cloud.mall.goods.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 类目服务。类目总量通常只有几十到几百条，树直接全量查出在内存里组装。
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final int ENABLED = 1;

    private final CategoryMapper categoryMapper;
    private final GoodsMapper goodsMapper;

    @Override
    public List<CategoryVO> adminTree() {
        return buildTree(categoryMapper.selectList(null), false);
    }

    @Override
    @Cacheable(cacheNames = GoodsCacheNames.CATEGORY_TREE, key = "'enabled'")
    public List<CategoryVO> enabledTree() {
        return buildTree(categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().eq(Category::getStatus, ENABLED)), true);
    }

    /**
     * 组装两级树并按 sort、id 排序。onlyRoots 为 true 时丢弃父类目不在结果中的孤儿节点
     * （一级类目停用后，其子类目在前台也不应出现）。返回可变 ArrayList，以便缓存反序列化。
     */
    static List<CategoryVO> buildTree(List<Category> all, boolean dropOrphans) {
        Comparator<CategoryVO> order = Comparator.comparing((CategoryVO c) -> c.getSort() == null ? 0 : c.getSort())
                .thenComparing(CategoryVO::getId);
        Map<Long, CategoryVO> roots = new LinkedHashMap<>();
        List<CategoryVO> children = new ArrayList<>();
        for (Category c : all) {
            CategoryVO vo = toVO(c);
            if (isRoot(c)) {
                vo.setChildren(new ArrayList<>());
                roots.put(vo.getId(), vo);
            } else {
                children.add(vo);
            }
        }
        List<CategoryVO> orphans = new ArrayList<>();
        for (CategoryVO child : children) {
            CategoryVO parent = roots.get(child.getParentId());
            if (parent != null) {
                parent.getChildren().add(child);
            } else if (!dropOrphans) {
                orphans.add(child);
            }
        }
        List<CategoryVO> tree = new ArrayList<>(roots.values());
        tree.addAll(orphans);
        tree.sort(order);
        for (CategoryVO root : tree) {
            if (root.getChildren() != null) {
                root.getChildren().sort(order);
            }
        }
        return tree;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = GoodsCacheNames.CATEGORY_TREE, allEntries = true),
            @CacheEvict(cacheNames = GoodsCacheNames.HOME, allEntries = true)})
    public Long create(CategoryRequest req) {
        long parentId = normalizeParent(req.getParentId());
        checkParent(null, parentId);
        Category c = new Category();
        apply(c, req, parentId);
        categoryMapper.insert(c);
        return c.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = GoodsCacheNames.CATEGORY_TREE, allEntries = true),
            @CacheEvict(cacheNames = GoodsCacheNames.HOME, allEntries = true)})
    public void update(Long id, CategoryRequest req) {
        Category c = mustGet(id);
        long parentId = normalizeParent(req.getParentId());
        if (!Objects.equals(parentId, c.getParentId())) {
            checkParent(id, parentId);
            // 一级改成二级：它的子类目会变成三级，不允许
            if (parentId != Category.ROOT_ID && hasChildren(id)) {
                throw new BusinessException("该类目下有子类目，不能移动到其他类目下");
            }
            // 二级改成一级：商品只能挂二级类目，已有商品时不允许
            if (parentId == Category.ROOT_ID && hasGoods(id)) {
                throw new BusinessException("该类目下有商品，不能改为一级类目");
            }
        }
        apply(c, req, parentId);
        if (categoryMapper.updateById(c) == 0) {
            throw new BusinessException("数据已被修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = GoodsCacheNames.CATEGORY_TREE, allEntries = true),
            @CacheEvict(cacheNames = GoodsCacheNames.HOME, allEntries = true)})
    public void delete(Long id) {
        mustGet(id);
        if (hasChildren(id)) {
            throw new BusinessException("请先删除子类目");
        }
        if (hasGoods(id)) {
            throw new BusinessException("该类目下有商品，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public List<Long> selfAndChildrenIds(Long categoryId) {
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .select(Category::getId)
                        .eq(Category::getParentId, categoryId))
                .forEach(c -> ids.add(c.getId()));
        return ids;
    }

    @Override
    public Map<Long, Long> parentIds(Collection<Long> categoryIds) {
        Map<Long, Long> result = new HashMap<>();
        List<Long> ids = categoryIds == null ? List.of() : categoryIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return result;
        }
        categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .select(Category::getId, Category::getParentId)
                        .in(Category::getId, ids))
                .forEach(c -> result.put(c.getId(), c.getParentId()));
        return result;
    }

    @Override
    public Map<Long, String> names(Collection<Long> categoryIds) {
        Map<Long, String> result = new HashMap<>();
        List<Long> ids = categoryIds == null ? List.of() : categoryIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return result;
        }
        categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .select(Category::getId, Category::getName)
                        .in(Category::getId, ids))
                .forEach(c -> result.put(c.getId(), c.getName()));
        return result;
    }

    private Category mustGet(Long id) {
        Category c = categoryMapper.selectById(id);
        if (c == null) {
            throw new BusinessException("类目不存在");
        }
        return c;
    }

    /** 父类目必须存在且是一级类目，保证最多两级 */
    private void checkParent(Long selfId, long parentId) {
        if (parentId == Category.ROOT_ID) {
            return;
        }
        if (Objects.equals(selfId, parentId)) {
            throw new BusinessException("父类目不能是自己");
        }
        Category parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException("父类目不存在");
        }
        if (!isRoot(parent)) {
            throw new BusinessException("类目最多两级，父类目必须是一级类目");
        }
    }

    private boolean hasChildren(Long id) {
        return categoryMapper.exists(new LambdaQueryWrapper<Category>().eq(Category::getParentId, id));
    }

    private boolean hasGoods(Long id) {
        return goodsMapper.exists(new LambdaQueryWrapper<Goods>().eq(Goods::getCategoryId, id));
    }

    private static long normalizeParent(Long parentId) {
        return parentId == null ? Category.ROOT_ID : parentId;
    }

    private static boolean isRoot(Category c) {
        return c.getParentId() == null || c.getParentId() == Category.ROOT_ID;
    }

    private static void apply(Category c, CategoryRequest req, long parentId) {
        c.setParentId(parentId);
        c.setName(req.getName().trim());
        c.setIcon(req.getIcon() == null ? "" : req.getIcon());
        c.setSort(req.getSort() == null ? 0 : req.getSort());
        c.setStatus(req.getStatus() == null ? ENABLED : req.getStatus());
    }

    private static CategoryVO toVO(Category c) {
        CategoryVO vo = new CategoryVO();
        vo.setId(c.getId());
        vo.setParentId(c.getParentId());
        vo.setName(c.getName());
        vo.setIcon(c.getIcon());
        vo.setSort(c.getSort());
        vo.setStatus(c.getStatus());
        return vo;
    }
}
