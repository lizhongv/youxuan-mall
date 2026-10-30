package com.platform.cloud.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.member.dto.LevelRequest;
import com.platform.cloud.mall.member.entity.MbrLevel;
import com.platform.cloud.mall.member.entity.MbrMember;
import com.platform.cloud.mall.member.mapper.MbrLevelMapper;
import com.platform.cloud.mall.member.mapper.MbrMemberMapper;
import com.platform.cloud.mall.member.service.MemberLevelService;
import com.platform.cloud.mall.member.vo.LevelVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 会员等级。成长值门槛必须唯一，否则同一成长值会匹配到两个等级。
 * <p>
 * 调整门槛不会批量重算已有会员的等级：会员下次增加成长值时按新门槛升级，避免一次改动触发全表更新。
 */
@Service
@RequiredArgsConstructor
public class MemberLevelServiceImpl implements MemberLevelService {

    private final MbrLevelMapper levelMapper;
    private final MbrMemberMapper memberMapper;

    @Override
    public PageVO<LevelVO> page(PageQuery q) {
        String kw = StringUtils.hasText(q.getKeyword()) ? q.getKeyword().trim() : null;
        LambdaQueryWrapper<MbrLevel> w = new LambdaQueryWrapper<MbrLevel>()
                .like(kw != null, MbrLevel::getName, kw)
                .orderByAsc(MbrLevel::getMinGrowth);
        return PageVO.of(levelMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w), MemberLevelServiceImpl::toVO);
    }

    @Override
    public List<LevelVO> list() {
        return levelMapper.selectList(new LambdaQueryWrapper<MbrLevel>().orderByAsc(MbrLevel::getMinGrowth))
                .stream().map(MemberLevelServiceImpl::toVO).toList();
    }

    @Override
    public LevelVO detail(Long id) {
        return toVO(require(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(LevelRequest req) {
        checkUnique(req, null);
        MbrLevel level = new MbrLevel();
        apply(level, req);
        levelMapper.insert(level);
        return level.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, LevelRequest req) {
        require(id);
        checkUnique(req, id);
        MbrLevel level = new MbrLevel();
        apply(level, req);
        levelMapper.update(null, new LambdaUpdateWrapper<MbrLevel>()
                .set(MbrLevel::getName, level.getName())
                .set(MbrLevel::getMinGrowth, level.getMinGrowth())
                .set(MbrLevel::getDiscount, level.getDiscount())
                .set(MbrLevel::getIcon, level.getIcon())
                .set(MbrLevel::getSort, level.getSort())
                .eq(MbrLevel::getId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        require(id);
        Long members = memberMapper.selectCount(new LambdaQueryWrapper<MbrMember>().eq(MbrMember::getLevelId, id));
        if (members != null && members > 0) {
            throw new BusinessException("该等级下还有 " + members + " 个会员，不能删除");
        }
        Long total = levelMapper.selectCount(null);
        if (total != null && total <= 1) {
            throw new BusinessException("至少保留一个会员等级");
        }
        levelMapper.deleteById(id);
    }

    @Override
    public MbrLevel defaultLevel() {
        return levelMapper.selectList(new LambdaQueryWrapper<MbrLevel>().orderByAsc(MbrLevel::getMinGrowth))
                .stream().findFirst().orElse(null);
    }

    @Override
    public MbrLevel matchLevel(int growth) {
        // 等级只有个位数，全量取出在内存里比较，避免依赖数据库方言的 LIMIT
        return levelMapper.selectList(new LambdaQueryWrapper<MbrLevel>().le(MbrLevel::getMinGrowth, growth))
                .stream().max(Comparator.comparing(MbrLevel::getMinGrowth)).orElse(null);
    }

    @Override
    public MbrLevel get(Long id) {
        return id == null ? null : levelMapper.selectById(id);
    }

    private void checkUnique(LevelRequest req, Long excludeId) {
        Long sameGrowth = levelMapper.selectCount(new LambdaQueryWrapper<MbrLevel>()
                .eq(MbrLevel::getMinGrowth, req.getMinGrowth())
                .ne(excludeId != null, MbrLevel::getId, excludeId));
        if (sameGrowth != null && sameGrowth > 0) {
            throw new BusinessException("已存在相同成长值门槛的等级");
        }
        Long sameName = levelMapper.selectCount(new LambdaQueryWrapper<MbrLevel>()
                .eq(MbrLevel::getName, req.getName().trim())
                .ne(excludeId != null, MbrLevel::getId, excludeId));
        if (sameName != null && sameName > 0) {
            throw new BusinessException("等级名称已存在");
        }
    }

    private static void apply(MbrLevel level, LevelRequest req) {
        level.setName(req.getName().trim());
        level.setMinGrowth(req.getMinGrowth());
        level.setDiscount(req.getDiscount().setScale(2, RoundingMode.HALF_UP));
        level.setIcon(Objects.toString(req.getIcon(), ""));
        level.setSort(req.getSort() == null ? 0 : req.getSort());
    }

    private MbrLevel require(Long id) {
        MbrLevel level = levelMapper.selectById(id);
        if (level == null) {
            throw new BusinessException("会员等级不存在");
        }
        return level;
    }

    static LevelVO toVO(MbrLevel l) {
        LevelVO vo = new LevelVO();
        vo.setId(l.getId());
        vo.setName(l.getName());
        vo.setMinGrowth(l.getMinGrowth());
        vo.setDiscount(l.getDiscount());
        vo.setIcon(l.getIcon());
        vo.setSort(l.getSort());
        return vo;
    }
}
