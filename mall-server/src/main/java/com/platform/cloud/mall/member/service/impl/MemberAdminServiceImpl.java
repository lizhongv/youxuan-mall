package com.platform.cloud.mall.member.service.impl;

import com.platform.cloud.mall.common.security.TokenVersionStore;
import com.platform.cloud.mall.common.security.UserType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.common.util.MaskUtils;
import com.platform.cloud.mall.member.dto.AdjustRequest;
import com.platform.cloud.mall.member.dto.MemberQuery;
import com.platform.cloud.mall.member.entity.MbrLevel;
import com.platform.cloud.mall.member.entity.MbrMember;
import com.platform.cloud.mall.member.mapper.MbrLevelMapper;
import com.platform.cloud.mall.member.mapper.MbrMemberMapper;
import com.platform.cloud.mall.member.service.AddressService;
import com.platform.cloud.mall.member.service.MemberAccountService;
import com.platform.cloud.mall.member.service.MemberAdminService;
import com.platform.cloud.mall.member.support.AccountType;
import com.platform.cloud.mall.member.support.MemberConstants;
import com.platform.cloud.mall.member.vo.AddressVO;
import com.platform.cloud.mall.member.vo.MemberAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台会员管理。手机号在列表和详情里都脱敏，后台运营不需要完整号码。
 */
@Service
@RequiredArgsConstructor
public class MemberAdminServiceImpl implements MemberAdminService {

    /** 单次调账上限：防止手滑多输几个 0，大额调整应分多次并留备注 */
    static final BigDecimal MAX_ADJUST = new BigDecimal("100000");

    private final MbrMemberMapper memberMapper;
    private final MbrLevelMapper levelMapper;
    private final MemberAccountService accountService;
    private final AddressService addressService;
    private final TokenVersionStore versionStore;

    @Override
    public PageVO<MemberAdminVO> page(MemberQuery q) {
        String kw = StringUtils.hasText(q.getKeyword()) ? q.getKeyword().trim() : null;
        LambdaQueryWrapper<MbrMember> w = new LambdaQueryWrapper<MbrMember>()
                .and(kw != null, x -> x.like(MbrMember::getMobile, kw).or().like(MbrMember::getNickname, kw))
                .eq(q.getLevelId() != null, MbrMember::getLevelId, q.getLevelId())
                .eq(q.getStatus() != null, MbrMember::getStatus, q.getStatus())
                .orderByDesc(MbrMember::getCreatedAt);
        Page<MbrMember> page = memberMapper.selectPage(new Page<>(q.getPage(), q.getSize()), w);
        Map<Long, MbrLevel> levels = levelsOf(page.getRecords().stream()
                .map(MbrMember::getLevelId).filter(id -> id != null).collect(Collectors.toSet()));
        return PageVO.of(page, m -> toVO(m, levels.get(m.getLevelId())));
    }

    @Override
    public MemberAdminVO detail(Long id) {
        MbrMember m = require(id);
        MemberAdminVO vo = toVO(m, m.getLevelId() == null ? null : levelMapper.selectById(m.getLevelId()));
        List<AddressVO> addresses = addressService.list(id);
        addresses.forEach(a -> a.setMobile(MaskUtils.mobile(a.getMobile())));
        vo.setAddresses(addresses);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != MemberConstants.ENABLED && status != MemberConstants.FROZEN)) {
            throw new BusinessException("状态只能是 1 或 0");
        }
        require(id);
        memberMapper.update(null, new LambdaUpdateWrapper<MbrMember>()
                .set(MbrMember::getStatus, status)
                .eq(MbrMember::getId, id));
        if (status == MemberConstants.FROZEN) {
            // 冻结立即生效：已登录的会话全部失效，而不是等令牌自然过期
            versionStore.revokeAll(UserType.MEMBER, id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjust(Long id, AdjustRequest req) {
        require(id);
        BigDecimal amount = req.getAmount();
        if (amount.signum() == 0) {
            throw new BusinessException("调整数额不能为 0");
        }
        if (amount.abs().compareTo(MAX_ADJUST) > 0) {
            throw new BusinessException("单次调整不能超过 " + MAX_ADJUST.toPlainString());
        }
        String remark = StringUtils.hasText(req.getRemark()) ? req.getRemark().trim() : "后台调整";
        if (AccountType.BALANCE.name().equals(req.getAccountType())) {
            if (amount.stripTrailingZeros().scale() > 2) {
                throw new BusinessException("余额最多两位小数");
            }
            accountService.changeBalance(id, amount, MemberConstants.BIZ_ADMIN_ADJUST, "", remark);
        } else {
            if (amount.stripTrailingZeros().scale() > 0) {
                throw new BusinessException("积分必须是整数");
            }
            accountService.changePoints(id, amount.intValueExact(), MemberConstants.BIZ_ADMIN_ADJUST, "", remark);
        }
    }

    private Map<Long, MbrLevel> levelsOf(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return levelMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(MbrLevel::getId, Function.identity()));
    }

    private MbrMember require(Long id) {
        MbrMember m = memberMapper.selectById(id);
        if (m == null) {
            throw new BusinessException("会员不存在");
        }
        return m;
    }

    private static MemberAdminVO toVO(MbrMember m, MbrLevel level) {
        MemberAdminVO vo = new MemberAdminVO();
        vo.setId(m.getId());
        vo.setMobile(MaskUtils.mobile(m.getMobile()));
        vo.setNickname(m.getNickname());
        vo.setAvatar(m.getAvatar());
        vo.setGender(m.getGender());
        vo.setBirthday(m.getBirthday());
        vo.setLevelId(m.getLevelId());
        vo.setLevelName(level == null ? "" : level.getName());
        vo.setPoints(m.getPoints());
        vo.setGrowth(m.getGrowth());
        vo.setBalance(m.getBalance());
        vo.setStatus(m.getStatus());
        vo.setLastLoginAt(m.getLastLoginAt());
        vo.setCreatedAt(m.getCreatedAt());
        return vo;
    }
}
