package com.platform.cloud.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.member.entity.MbrAddress;
import com.platform.cloud.mall.member.entity.MbrBrowseHistory;
import com.platform.cloud.mall.member.entity.MbrFavorite;
import com.platform.cloud.mall.member.entity.MbrLevel;
import com.platform.cloud.mall.member.entity.MbrMember;
import com.platform.cloud.mall.member.entity.MbrSocial;
import com.platform.cloud.mall.member.mapper.MbrAddressMapper;
import com.platform.cloud.mall.member.mapper.MbrBrowseHistoryMapper;
import com.platform.cloud.mall.member.mapper.MbrFavoriteMapper;
import com.platform.cloud.mall.member.mapper.MbrLevelMapper;
import com.platform.cloud.mall.member.mapper.MbrMemberMapper;
import com.platform.cloud.mall.member.mapper.MbrSocialMapper;
import com.platform.cloud.mall.member.service.MemberAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 会员领域对外开放接口的实现。
 * <p>
 * 刻意只依赖本领域的 Mapper 与账户服务，不依赖商品 / 交易 / 营销的接口：
 * 那些领域的实现会反过来注入 MemberApi，这样可以避免构造器注入形成循环依赖。
 */
@Service
@RequiredArgsConstructor
public class MemberApiImpl implements MemberApi {

    /** 每个会员保留的足迹上限，超出删除最早的；200 条足够「最近看过」的场景 */
    static final int MAX_BROWSE_HISTORY = 200;

    private static final BigDecimal NO_DISCOUNT = new BigDecimal("1.00");

    private final MbrMemberMapper memberMapper;
    private final MbrLevelMapper levelMapper;
    private final MbrAddressMapper addressMapper;
    private final MbrFavoriteMapper favoriteMapper;
    private final MbrBrowseHistoryMapper historyMapper;
    private final MemberAccountService accountService;
    private final MbrSocialMapper socialMapper;

    @Override
    public MemberBrief getMember(Long memberId) {
        if (memberId == null) {
            return null;
        }
        MbrMember m = memberMapper.selectById(memberId);
        if (m == null) {
            return null;
        }
        MbrLevel level = m.getLevelId() == null ? null : levelMapper.selectById(m.getLevelId());
        return toBrief(m, level);
    }

    @Override
    public Map<Long, MemberBrief> getMembers(Collection<Long> memberIds) {
        Set<Long> ids = new HashSet<>(memberIds == null ? List.of() : memberIds);
        ids.remove(null);
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<MbrMember> members = memberMapper.selectBatchIds(ids);
        Set<Long> levelIds = members.stream().map(MbrMember::getLevelId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, MbrLevel> levels = levelIds.isEmpty() ? Map.of()
                : levelMapper.selectBatchIds(levelIds).stream()
                .collect(Collectors.toMap(MbrLevel::getId, Function.identity()));
        return members.stream().collect(Collectors.toMap(MbrMember::getId, m -> toBrief(m, levels.get(m.getLevelId()))));
    }

    @Override
    public AddressSnapshot getAddress(Long memberId, Long addressId) {
        MbrAddress a = addressId == null ? null : addressMapper.selectOne(new LambdaQueryWrapper<MbrAddress>()
                .eq(MbrAddress::getId, addressId)
                .eq(MbrAddress::getMemberId, memberId));
        if (a == null) {
            throw new BusinessException("收货地址不存在");
        }
        return new AddressSnapshot(a.getName(), a.getMobile(), a.getProvince(), a.getCity(), a.getDistrict(),
                a.getDetail());
    }

    @Override
    public void changeBalance(Long memberId, BigDecimal delta, String bizType, String bizNo, String remark) {
        accountService.changeBalance(memberId, delta, bizType, bizNo, remark);
    }

    @Override
    public void changePoints(Long memberId, int delta, String bizType, String bizNo, String remark) {
        accountService.changePoints(memberId, delta, bizType, bizNo, remark);
    }

    @Override
    public void addGrowth(Long memberId, int delta, String bizType, String bizNo, String remark) {
        accountService.addGrowth(memberId, delta, bizType, bizNo, remark);
    }

    @Override
    public boolean isFavorited(Long memberId, Long goodsId) {
        if (memberId == null || goodsId == null) {
            return false;
        }
        Long n = favoriteMapper.selectCount(new LambdaQueryWrapper<MbrFavorite>()
                .eq(MbrFavorite::getMemberId, memberId)
                .eq(MbrFavorite::getGoodsId, goodsId));
        return n != null && n > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordBrowse(Long memberId, Long goodsId) {
        if (memberId == null || goodsId == null) {
            return;
        }
        int touched = historyMapper.update(null, new LambdaUpdateWrapper<MbrBrowseHistory>()
                .set(MbrBrowseHistory::getUpdatedAt, LocalDateTime.now())
                .eq(MbrBrowseHistory::getMemberId, memberId)
                .eq(MbrBrowseHistory::getGoodsId, goodsId));
        if (touched > 0) {
            return;
        }
        MbrBrowseHistory h = new MbrBrowseHistory();
        h.setMemberId(memberId);
        h.setGoodsId(goodsId);
        historyMapper.insert(h);
        trimHistory(memberId);
    }

    @Override
    public String getOpenid(Long memberId, String platform) {
        if (memberId == null) {
            return null;
        }
        MbrSocial s = socialMapper.selectOne(new LambdaQueryWrapper<MbrSocial>()
                .eq(MbrSocial::getMemberId, memberId)
                .eq(MbrSocial::getPlatform, platform));
        return s == null ? null : s.getOpenid();
    }

    @Override
    public long countRegistered(LocalDateTime from, LocalDateTime to) {
        Long n = memberMapper.selectCount(new LambdaQueryWrapper<MbrMember>()
                .ge(from != null, MbrMember::getCreatedAt, from)
                .lt(to != null, MbrMember::getCreatedAt, to));
        return n == null ? 0 : n;
    }

    /** 只在新增足迹时检查，刷新已有足迹不会让数量变化 */
    private void trimHistory(Long memberId) {
        LambdaQueryWrapper<MbrBrowseHistory> byMember = new LambdaQueryWrapper<MbrBrowseHistory>()
                .eq(MbrBrowseHistory::getMemberId, memberId);
        Long total = historyMapper.selectCount(byMember);
        long overflow = (total == null ? 0 : total) - MAX_BROWSE_HISTORY;
        if (overflow <= 0) {
            return;
        }
        List<Long> oldest = historyMapper.selectPage(new Page<>(1, overflow, false),
                        new LambdaQueryWrapper<MbrBrowseHistory>()
                                .select(MbrBrowseHistory::getId)
                                .eq(MbrBrowseHistory::getMemberId, memberId)
                                .orderByAsc(MbrBrowseHistory::getUpdatedAt))
                .getRecords().stream().map(MbrBrowseHistory::getId).toList();
        if (!oldest.isEmpty()) {
            historyMapper.deleteBatchIds(oldest);
        }
    }

    private static MemberBrief toBrief(MbrMember m, MbrLevel level) {
        return new MemberBrief(m.getId(), m.getMobile(), m.getNickname(), m.getAvatar(), m.getLevelId(),
                level == null ? "" : level.getName(),
                level == null ? NO_DISCOUNT : level.getDiscount(),
                m.getBalance(), m.getPoints(), m.getStatus());
    }
}
