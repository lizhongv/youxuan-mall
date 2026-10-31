package com.platform.cloud.bootstrap.goods.brand.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.bootstrap.goods.brand.converter.BrandConverter;
import com.platform.cloud.bootstrap.goods.brand.dto.BrandUpsertRequest;
import com.platform.cloud.bootstrap.goods.brand.entity.BrandEntity;
import com.platform.cloud.bootstrap.goods.brand.mapper.BrandMapper;
import com.platform.cloud.bootstrap.goods.brand.vo.BrandVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private static final String CACHE_ITEM = "goods.brand.item";

    private final BrandMapper mapper;
    private final BrandConverter converter;

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_ITEM, allEntries = true)
    public Long create(BrandUpsertRequest req) {
        Long exists = mapper.selectCount(new LambdaQueryWrapper<BrandEntity>()
                .eq(BrandEntity::getName, req.getName()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "品牌名已存在: " + req.getName());
        }
        BrandEntity entity = converter.toEntity(req);
        mapper.insert(entity);
        log.info("Brand created id={} name={}", entity.getId(), entity.getName());
        return entity.getId();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_ITEM, allEntries = true)
    public void update(Long id, BrandUpsertRequest req) {
        BrandEntity entity = mustGet(id);
        if (req.getName() != null && !req.getName().equals(entity.getName())) {
            Long sameName = mapper.selectCount(new LambdaQueryWrapper<BrandEntity>()
                    .eq(BrandEntity::getName, req.getName())
                    .ne(BrandEntity::getId, id));
            if (sameName != null && sameName > 0) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "品牌名已存在: " + req.getName());
            }
        }
        converter.updateEntity(req, entity);
        mapper.updateById(entity);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_ITEM, allEntries = true)
    public void delete(Long id) {
        mustGet(id);
        mapper.deleteById(id);
    }

    @Override
    @Cacheable(cacheNames = CACHE_ITEM, key = "#id")
    public BrandVO getById(Long id) {
        return converter.toVO(mustGet(id));
    }

    @Override
    public PageVO<BrandVO> page(PageQuery query) {
        LambdaQueryWrapper<BrandEntity> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            q.like(BrandEntity::getName, query.getKeyword());
        }
        q.orderByAsc(BrandEntity::getSortOrder).orderByDesc(BrandEntity::getId);
        Page<BrandEntity> page = mapper.selectPage(new Page<>(query.getPage(), query.getSize()), q);
        return PageVO.of(page, converter::toVO);
    }

    private BrandEntity mustGet(Long id) {
        BrandEntity e = mapper.selectById(id);
        if (e == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "品牌不存在: " + id);
        }
        return e;
    }
}
