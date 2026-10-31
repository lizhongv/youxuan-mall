package com.platform.cloud.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.security.JwtProperties;
import com.platform.cloud.bootstrap.common.security.JwtService;
import com.platform.cloud.bootstrap.common.security.TokenBlacklist;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.mall.common.security.LoginAttemptGuard;
import com.platform.cloud.mall.common.security.TokenVersionStore;
import com.platform.cloud.mall.common.security.UserType;
import com.platform.cloud.mall.common.util.RequestUtils;
import com.platform.cloud.mall.member.dto.MemberLoginRequest;
import com.platform.cloud.mall.member.dto.MemberRegisterRequest;
import com.platform.cloud.mall.member.dto.MemberResetPasswordRequest;
import com.platform.cloud.mall.member.dto.SmsLoginRequest;
import com.platform.cloud.mall.member.entity.MbrLevel;
import com.platform.cloud.mall.member.entity.MbrMember;
import com.platform.cloud.mall.member.mapper.MbrMemberMapper;
import com.platform.cloud.mall.member.service.MemberAccountService;
import com.platform.cloud.mall.member.service.MemberAuthService;
import com.platform.cloud.mall.member.service.MemberLevelService;
import com.platform.cloud.mall.member.service.SmsCodeService;
import com.platform.cloud.mall.member.support.MemberConstants;
import com.platform.cloud.mall.member.support.SmsScene;
import com.platform.cloud.mall.member.vo.MemberTokenVO;
import com.platform.cloud.mall.system.api.ConfigApi;
import com.platform.cloud.mall.system.api.ConfigKeys;
import com.platform.cloud.mall.system.service.SysLoginLogService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商城端认证。会员令牌 roles 只有 ROLE_MEMBER，refresh 令牌带 utype=MEMBER，与管理端令牌互不通用。
 * 令牌带 tv（版本号）声明：改密、重置密码、冻结后旧令牌立即失效，见 {@link TokenVersionStore}。
 */
@Service
@RequiredArgsConstructor
public class MemberAuthServiceImpl implements MemberAuthService {

    private final MbrMemberMapper memberMapper;
    private final MemberLevelService levelService;
    private final MemberAccountService accountService;
    private final SmsCodeService smsCodeService;
    private final LoginAttemptGuard attemptGuard;
    private final ConfigApi configApi;
    private final SysLoginLogService loginLogService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final TokenBlacklist tokenBlacklist;
    private final TokenVersionStore versionStore;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberTokenVO register(MemberRegisterRequest req) {
        String mobile = req.getMobile();
        smsCodeService.verify(mobile, SmsScene.REGISTER, req.getCode());
        // 发送验证码时不提示「已注册」（防止手机号枚举），只有拿到验证码的本人才会看到这条提示
        if (findByMobile(mobile) != null) {
            throw new BusinessException("该手机号已注册，请直接登录");
        }
        MbrMember m = createMember(mobile, passwordEncoder.encode(req.getPassword()), req.getNickname());
        loginLogService.record(UserType.MEMBER, m.getId(), mobile, true, "注册并登录");
        return issueTokens(m);
    }

    @Override
    public MemberTokenVO login(MemberLoginRequest req) {
        String mobile = req.getMobile();
        LoginAttemptGuard.Attempt attempt = attemptGuard.begin(LoginAttemptGuard.Scene.MEMBER, mobile,
                RequestUtils.clientIp(), req.getCaptchaId(), req.getCaptchaCode());
        MbrMember m = findByMobile(mobile);
        if (m != null && !StringUtils.hasText(m.getPassword())) {
            attemptGuard.release(attempt);
            throw new BusinessException("该账号尚未设置密码，请使用验证码登录");
        }
        if (m == null || !passwordEncoder.matches(req.getPassword(), m.getPassword())) {
            BusinessException failure = attemptGuard.onFailure(attempt);
            loginLogService.record(UserType.MEMBER, m == null ? null : m.getId(), mobile, false, failure.getMessage());
            throw failure;
        }
        if (!isActive(m)) {
            attemptGuard.release(attempt);
            rejectFrozen(m);
        }
        attemptGuard.onSuccess(attempt);
        return onLoginSuccess(m, "密码登录");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberTokenVO loginBySms(SmsLoginRequest req) {
        String mobile = req.getMobile();
        smsCodeService.verify(mobile, SmsScene.LOGIN, req.getCode());
        MbrMember m = findByMobile(mobile);
        if (m == null) {
            m = createMember(mobile, "", null);
            loginLogService.record(UserType.MEMBER, m.getId(), mobile, true, "验证码注册并登录");
            return issueTokens(m);
        }
        if (!isActive(m)) {
            rejectFrozen(m);
        }
        // 能收到验证码说明是本人，顺便解除本 IP 上密码登录的锁定
        attemptGuard.reset(LoginAttemptGuard.Scene.MEMBER, mobile, RequestUtils.clientIp());
        return onLoginSuccess(m, "验证码登录");
    }

    @Override
    public MemberTokenVO refresh(String refreshToken) {
        Claims claims = jwtService.parseQuiet(refreshToken);
        if (claims == null
                || !JwtService.TYPE_REFRESH.equals(claims.get(JwtService.CLAIM_TYPE))
                || !UserType.MEMBER.name().equals(claims.get(MemberConstants.CLAIM_UTYPE))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Long memberId;
        try {
            memberId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        MbrMember m = memberMapper.selectById(memberId);
        if (m == null || !isActive(m)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在或已被冻结");
        }
        // 改密 / 冻结后版本号已变；Redis 异常时直接抛出（严格失败）
        if (!versionStore.matches(UserType.MEMBER, memberId, claims)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
        // refresh 令牌轮换：SET NX 原子地标记已使用，并发重放只有一个能成功
        if (!tokenBlacklist.tryConsume(claims.getId(), remaining(claims))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return issueTokens(m);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(MemberResetPasswordRequest req) {
        String mobile = req.getMobile();
        smsCodeService.verify(mobile, SmsScene.RESET, req.getCode());
        MbrMember m = findByMobile(mobile);
        if (m == null) {
            throw new BusinessException("该手机号未注册");
        }
        memberMapper.update(null, new LambdaUpdateWrapper<MbrMember>()
                .set(MbrMember::getPassword, passwordEncoder.encode(req.getPassword()))
                .set(MbrMember::getPwdChangedAt, LocalDateTime.now())
                .eq(MbrMember::getId, m.getId()));
        // 忘记密码往往意味着账号可能已泄露：所有设备上的登录一并失效
        versionStore.revokeAll(UserType.MEMBER, m.getId());
        attemptGuard.reset(LoginAttemptGuard.Scene.MEMBER, mobile, RequestUtils.clientIp());
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        if (StringUtils.hasText(accessToken)) {
            Claims claims = jwtService.parseQuiet(accessToken);
            if (claims != null) {
                revoke(claims);
            }
        }
        // refresh 令牌也拉黑，否则登出后仍能用它换新令牌
        if (StringUtils.hasText(refreshToken)) {
            Claims claims = jwtService.parseQuiet(refreshToken);
            if (claims != null && JwtService.TYPE_REFRESH.equals(claims.get(JwtService.CLAIM_TYPE))
                    && UserType.MEMBER.name().equals(claims.get(MemberConstants.CLAIM_UTYPE))) {
                revoke(claims);
            }
        }
    }

    @Override
    public MemberTokenVO issueTokens(Long memberId) {
        MbrMember m = memberMapper.selectById(memberId);
        if (m == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在");
        }
        return issueTokens(m);
    }

    /**
     * 新会员：默认等级为门槛最低的等级，昵称默认「用户 + 手机后四位」，并按系统参数赠送余额（默认 0）。
     */
    MbrMember createMember(String mobile, String encodedPassword, String nickname) {
        MbrLevel level = levelService.defaultLevel();
        MbrMember m = new MbrMember();
        m.setMobile(mobile);
        m.setPassword(encodedPassword);
        m.setNickname(StringUtils.hasText(nickname) ? nickname.trim() : defaultNickname(mobile));
        m.setAvatar("");
        m.setGender(0);
        m.setLevelId(level == null ? null : level.getId());
        m.setPoints(0);
        m.setGrowth(0);
        m.setBalance(BigDecimal.ZERO);
        m.setStatus(MemberConstants.ENABLED);
        m.setRegisterIp(RequestUtils.clientIp());
        m.setLastLoginAt(LocalDateTime.now());
        m.setTokenVersion(0);
        try {
            memberMapper.insert(m);
        } catch (DuplicateKeyException e) {
            // 同一手机号并发注册，唯一索引兜底
            throw new BusinessException("该手机号已注册，请直接登录");
        }
        BigDecimal gift = configApi.getDecimal(ConfigKeys.REGISTER_GIFT_BALANCE, BigDecimal.ZERO);
        if (gift != null && gift.signum() > 0) {
            accountService.changeBalance(m.getId(), gift, MemberConstants.BIZ_REGISTER_GIFT, "", "新会员注册赠送");
        }
        return m;
    }

    static String defaultNickname(String mobile) {
        String tail = mobile.length() >= 4 ? mobile.substring(mobile.length() - 4) : mobile;
        return MemberConstants.DEFAULT_NICKNAME_PREFIX + tail;
    }

    private MemberTokenVO onLoginSuccess(MbrMember m, String message) {
        memberMapper.update(null, new LambdaUpdateWrapper<MbrMember>()
                .set(MbrMember::getLastLoginAt, LocalDateTime.now())
                .eq(MbrMember::getId, m.getId()));
        loginLogService.record(UserType.MEMBER, m.getId(), m.getMobile(), true, message);
        return issueTokens(m);
    }

    private static boolean isActive(MbrMember m) {
        return m.getStatus() != null && m.getStatus() == MemberConstants.ENABLED;
    }

    private void rejectFrozen(MbrMember m) {
        loginLogService.record(UserType.MEMBER, m.getId(), m.getMobile(), false, "账号已冻结");
        throw new BusinessException("账号已被冻结，请联系客服");
    }

    private MbrMember findByMobile(String mobile) {
        return memberMapper.selectOne(new LambdaQueryWrapper<MbrMember>().eq(MbrMember::getMobile, mobile));
    }

    MemberTokenVO issueTokens(MbrMember m) {
        String subject = String.valueOf(m.getId());
        int tv = m.getTokenVersion() == null ? 0 : m.getTokenVersion();
        Duration accessTtl = jwtProperties.getAccessTtl();
        String access = jwtService.generate(subject, JwtService.TYPE_ACCESS, Map.of(
                JwtService.CLAIM_ROLES, List.of(UserType.MEMBER.role()),
                MemberConstants.CLAIM_UTYPE, UserType.MEMBER.name(),
                JwtService.CLAIM_TOKEN_VERSION, tv), accessTtl);
        String refresh = jwtService.generate(subject, JwtService.TYPE_REFRESH, Map.of(
                MemberConstants.CLAIM_UTYPE, UserType.MEMBER.name(),
                JwtService.CLAIM_TOKEN_VERSION, tv), jwtProperties.getRefreshTtl());
        return new MemberTokenVO(access, refresh, (int) accessTtl.toSeconds());
    }

    private void revoke(Claims claims) {
        if (claims.getExpiration() == null) {
            return;
        }
        tokenBlacklist.revoke(claims.getId(), remaining(claims));
    }

    private static Duration remaining(Claims claims) {
        if (claims.getExpiration() == null) {
            return Duration.ZERO;
        }
        return Duration.between(Instant.now(), claims.getExpiration().toInstant());
    }
}
