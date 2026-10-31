package com.platform.cloud.mall.member.service.impl;

import com.platform.cloud.mall.common.security.TokenVersionStore;
import com.platform.cloud.mall.common.security.UserType;
import com.platform.cloud.mall.member.service.MemberAuthService;
import com.platform.cloud.mall.member.vo.MemberTokenVO;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.mall.common.util.MaskUtils;
import com.platform.cloud.mall.marketing.api.CouponApi;
import com.platform.cloud.mall.member.dto.MemberPasswordRequest;
import com.platform.cloud.mall.member.dto.MemberProfileRequest;
import com.platform.cloud.mall.member.entity.MbrLevel;
import com.platform.cloud.mall.member.entity.MbrMember;
import com.platform.cloud.mall.member.mapper.MbrMemberMapper;
import com.platform.cloud.mall.member.service.FavoriteService;
import com.platform.cloud.mall.member.service.MemberLevelService;
import com.platform.cloud.mall.member.service.MemberProfileService;
import com.platform.cloud.mall.member.vo.MemberProfileVO;
import com.platform.cloud.mall.trade.api.TradeApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员中心个人资料。订单角标、券数量来自交易与营销领域，通过其开放接口获取。
 */
@Service
@RequiredArgsConstructor
public class MemberProfileServiceImpl implements MemberProfileService {

    /** 会员中心固定展示的订单角标，交易领域没返回的键补 0，前端不用判空 */
    static final List<String> ORDER_COUNT_KEYS =
            List.of("PENDING_PAY", "PENDING_DELIVERY", "DELIVERED", "TO_REVIEW", "AFTERSALE");

    private final MbrMemberMapper memberMapper;
    private final MemberLevelService levelService;
    private final FavoriteService favoriteService;
    private final TradeApi tradeApi;
    private final CouponApi couponApi;
    private final PasswordEncoder passwordEncoder;
    private final TokenVersionStore versionStore;
    private final MemberAuthService authService;

    @Override
    public MemberProfileVO profile(Long memberId) {
        MbrMember m = require(memberId);
        MbrLevel level = levelService.get(m.getLevelId());

        MemberProfileVO vo = new MemberProfileVO();
        vo.setId(m.getId());
        vo.setMobile(MaskUtils.mobile(m.getMobile()));
        vo.setNickname(m.getNickname());
        vo.setAvatar(m.getAvatar());
        vo.setGender(m.getGender());
        vo.setBirthday(m.getBirthday());
        vo.setLevelName(level == null ? "" : level.getName());
        vo.setDiscount(level == null ? BigDecimal.ONE.setScale(2) : level.getDiscount());
        vo.setPoints(m.getPoints());
        vo.setGrowth(m.getGrowth());
        vo.setBalance(m.getBalance());
        vo.setHasPassword(StringUtils.hasText(m.getPassword()));
        vo.setCouponCount(toInt(couponApi.countUsable(memberId)));
        vo.setFavoriteCount(toInt(favoriteService.count(memberId)));
        vo.setOrderCount(normalizeOrderCounts(tradeApi.memberOrderCounts(memberId)));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long memberId, MemberProfileRequest req) {
        require(memberId);
        if (req.getNickname() != null && !StringUtils.hasText(req.getNickname())) {
            throw new BusinessException("昵称不能为空");
        }
        memberMapper.update(null, new LambdaUpdateWrapper<MbrMember>()
                .set(req.getNickname() != null, MbrMember::getNickname,
                        req.getNickname() == null ? null : req.getNickname().trim())
                .set(req.getAvatar() != null, MbrMember::getAvatar, req.getAvatar())
                .set(req.getGender() != null, MbrMember::getGender, req.getGender())
                .set(req.getBirthday() != null, MbrMember::getBirthday, req.getBirthday())
                .set(MbrMember::getUpdatedBy, memberId)
                .eq(MbrMember::getId, memberId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberTokenVO changePassword(Long memberId, MemberPasswordRequest req) {
        MbrMember m = require(memberId);
        if (StringUtils.hasText(m.getPassword())) {
            if (!StringUtils.hasText(req.getOldPassword())
                    || !passwordEncoder.matches(req.getOldPassword(), m.getPassword())) {
                throw new BusinessException("原密码错误");
            }
            if (req.getOldPassword().equals(req.getNewPassword())) {
                throw new BusinessException("新密码不能与原密码相同");
            }
        }
        memberMapper.update(null, new LambdaUpdateWrapper<MbrMember>()
                .set(MbrMember::getPassword, passwordEncoder.encode(req.getNewPassword()))
                .set(MbrMember::getPwdChangedAt, LocalDateTime.now())
                .eq(MbrMember::getId, memberId));
        // 其他设备上的登录（可能是盗号者）全部下线，当前会话换发新令牌
        versionStore.revokeAll(UserType.MEMBER, memberId);
        return authService.issueTokens(memberId);
    }

    static Map<String, Integer> normalizeOrderCounts(Map<String, Long> raw) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String key : ORDER_COUNT_KEYS) {
            Long v = raw == null ? null : raw.get(key);
            result.put(key, v == null ? 0 : toInt(v));
        }
        return result;
    }

    private static int toInt(long v) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, v));
    }

    private MbrMember require(Long memberId) {
        MbrMember m = memberMapper.selectById(memberId);
        if (m == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在");
        }
        return m;
    }
}
