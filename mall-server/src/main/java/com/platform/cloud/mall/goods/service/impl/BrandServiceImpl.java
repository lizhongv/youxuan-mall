package com.platform.cloud.mall.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.goods.dto.BrandQuery;
import com.platform.cloud.mall.goods.dto.BrandRequest;
import com.platform.cloud.mall.goods.entity.Brand;
import com.platform.cloud.mall.goods.entity.Goods;
import com.platform.cloud.mall.goods.mapper.BrandMapper;
import com.platform.cloud.mall.goods.mapper.GoodsMapper;
import com.platform.cloud.mall.goods.service.BrandService;
import com.platform.cloud.mall.goods.vo.BrandVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 品牌服务。
 */
@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private static final int ENABLED = 1;

    private final BrandMapper brandMapper;
    private final GoodsMapper goodsMapper;

    @Override
    public PageVO<BrandVO> page(BrandQuery q) {
        LambdaQueryWrapper<Brand> w = new LambdaQueryWrapper<Brand>()
                .like(StringUtils.hasText(q.getKeyword()), Brand::getName, q.getKeyword())
                .eq(q.getStatus() != null, Brand::getStatus, q.getStatus())
                .orderByAsc(Brand::getSort).orderByDesc(Brand::getId);
        return PageVO.of(brandMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w), BrandServiceImpl::toVO);
    }

    @Override
    public List<BrandVO> listEnabled() {
        return brandMapper.selectList(new LambdaQueryWrapper<Brand>()
                        .eq(Brand::getStatus, ENABLED)
                        .orderByAsc(Brand::getFirstLetter).orderByAsc(Brand::getSort))
                .stream().map(BrandServiceImpl::toVO).toList();
    }

    @Override
    public BrandVO get(Long id) {
        return toVO(mustGet(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(BrandRequest req) {
        Brand b = new Brand();
        apply(b, req);
        brandMapper.insert(b);
        return b.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BrandRequest req) {
        Brand b = mustGet(id);
        apply(b, req);
        if (brandMapper.updateById(b) == 0) {
            throw new BusinessException("数据已被修改，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        mustGet(id);
        if (goodsMapper.exists(new LambdaQueryWrapper<Goods>().eq(Goods::getBrandId, id))) {
            throw new BusinessException("该品牌下有商品，不能删除");
        }
        brandMapper.deleteById(id);
    }

    @Override
    public Map<Long, String> names(Collection<Long> brandIds) {
        Map<Long, String> result = new HashMap<>();
        List<Long> ids = brandIds == null ? List.of() : brandIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return result;
        }
        brandMapper.selectList(new LambdaQueryWrapper<Brand>()
                        .select(Brand::getId, Brand::getName)
                        .in(Brand::getId, ids))
                .forEach(b -> result.put(b.getId(), b.getName()));
        return result;
    }

    private Brand mustGet(Long id) {
        Brand b = brandMapper.selectById(id);
        if (b == null) {
            throw new BusinessException("品牌不存在");
        }
        return b;
    }

    private static void apply(Brand b, BrandRequest req) {
        b.setName(req.getName().trim());
        b.setLogo(req.getLogo() == null ? "" : req.getLogo());
        b.setFirstLetter(StringUtils.hasText(req.getFirstLetter()) ? req.getFirstLetter().toUpperCase(Locale.ROOT) : "");
        b.setDescription(req.getDescription() == null ? "" : req.getDescription());
        b.setSort(req.getSort() == null ? 0 : req.getSort());
        b.setStatus(req.getStatus() == null ? ENABLED : req.getStatus());
    }

    static BrandVO toVO(Brand b) {
        BrandVO vo = new BrandVO();
        vo.setId(b.getId());
        vo.setName(b.getName());
        vo.setLogo(b.getLogo());
        vo.setFirstLetter(b.getFirstLetter());
        vo.setDescription(b.getDescription());
        vo.setSort(b.getSort());
        vo.setStatus(b.getStatus());
        return vo;
    }
}
