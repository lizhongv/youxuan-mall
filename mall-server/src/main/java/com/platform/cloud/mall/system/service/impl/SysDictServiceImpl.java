package com.platform.cloud.mall.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.system.dto.DictQuery;
import com.platform.cloud.mall.system.dto.DictSaveRequest;
import com.platform.cloud.mall.system.entity.SysDict;
import com.platform.cloud.mall.system.mapper.SysDictMapper;
import com.platform.cloud.mall.system.service.SysDictService;
import com.platform.cloud.mall.system.support.SystemConstants;
import com.platform.cloud.mall.system.vo.DictItemVO;
import com.platform.cloud.mall.system.vo.DictVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 数据字典。同一类型下 value 唯一，否则前端按 value 回显会出现歧义。
 */
@Service
@RequiredArgsConstructor
public class SysDictServiceImpl implements SysDictService {

    private final SysDictMapper dictMapper;

    @Override
    public PageVO<DictVO> page(DictQuery q) {
        String kw = StringUtils.hasText(q.getKeyword()) ? q.getKeyword().trim() : null;
        LambdaQueryWrapper<SysDict> w = new LambdaQueryWrapper<SysDict>()
                .eq(StringUtils.hasText(q.getDictType()), SysDict::getDictType, q.getDictType())
                .eq(q.getStatus() != null, SysDict::getStatus, q.getStatus())
                .and(kw != null, x -> x.like(SysDict::getLabel, kw).or().like(SysDict::getValue, kw))
                .orderByAsc(SysDict::getDictType).orderByAsc(SysDict::getSort).orderByAsc(SysDict::getId);
        return PageVO.of(dictMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w), SysDictServiceImpl::toVO);
    }

    @Override
    public DictVO detail(Long id) {
        return toVO(require(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DictSaveRequest req) {
        checkUnique(req.getDictType(), req.getValue(), null);
        SysDict d = new SysDict();
        d.setDictType(req.getDictType());
        d.setLabel(req.getLabel().trim());
        d.setValue(req.getValue().trim());
        d.setSort(req.getSort() == null ? 0 : req.getSort());
        d.setStatus(req.getStatus() == null ? SystemConstants.ENABLED : req.getStatus());
        d.setRemark(Objects.toString(req.getRemark(), ""));
        dictMapper.insert(d);
        return d.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DictSaveRequest req) {
        require(id);
        checkUnique(req.getDictType(), req.getValue(), id);
        dictMapper.update(null, new LambdaUpdateWrapper<SysDict>()
                .set(SysDict::getDictType, req.getDictType())
                .set(SysDict::getLabel, req.getLabel().trim())
                .set(SysDict::getValue, req.getValue().trim())
                .set(req.getSort() != null, SysDict::getSort, req.getSort())
                .set(req.getStatus() != null, SysDict::getStatus, req.getStatus())
                .set(req.getRemark() != null, SysDict::getRemark, req.getRemark())
                .eq(SysDict::getId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        require(id);
        dictMapper.deleteById(id);
    }

    @Override
    public List<String> types() {
        return dictMapper.selectObjs(new QueryWrapper<SysDict>()
                        .select("DISTINCT dict_type")
                        .orderByAsc("dict_type"))
                .stream().map(String::valueOf).toList();
    }

    @Override
    public List<DictItemVO> items(String dictType) {
        return dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                        .eq(SysDict::getDictType, dictType)
                        .eq(SysDict::getStatus, SystemConstants.ENABLED)
                        .orderByAsc(SysDict::getSort).orderByAsc(SysDict::getId))
                .stream().map(d -> new DictItemVO(d.getLabel(), d.getValue())).toList();
    }

    private void checkUnique(String dictType, String value, Long excludeId) {
        Long count = dictMapper.selectCount(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, dictType)
                .eq(SysDict::getValue, value.trim())
                .ne(excludeId != null, SysDict::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("该字典类型下已存在相同的值");
        }
    }

    private SysDict require(Long id) {
        SysDict d = dictMapper.selectById(id);
        if (d == null) {
            throw new BusinessException("字典项不存在");
        }
        return d;
    }

    private static DictVO toVO(SysDict d) {
        DictVO vo = new DictVO();
        vo.setId(d.getId());
        vo.setDictType(d.getDictType());
        vo.setLabel(d.getLabel());
        vo.setValue(d.getValue());
        vo.setSort(d.getSort());
        vo.setStatus(d.getStatus());
        vo.setRemark(d.getRemark());
        return vo;
    }
}
