package com.platform.cloud.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.mall.common.security.RedisCounter;
import com.platform.cloud.mall.member.dto.PayPasswordRequest;
import com.platform.cloud.mall.member.entity.MbrMember;
import com.platform.cloud.mall.member.mapper.MbrMemberMapper;
import com.platform.cloud.mall.member.service.PayPasswordService;
import com.platform.cloud.mall.member.service.SmsCodeService;
import com.platform.cloud.mall.member.support.SmsScene;
import com.platform.cloud.mall.member.vo.SmsCodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 余额支付密码。
 * <p>
 * 错误计数放 Redis（mall:pay-pwd-fail:{memberId}）：校验发生在扣余额的事务之外，计数必须不随订单事务回滚；
 * 每次失败刷新 30 分钟过期，第 5 次失败即进入锁定，锁定期从最后一次失败算起 30 分钟。
 * 校验前先读计数判断锁定，再用 INCR 原子预占一次尝试（同 LoginAttemptGuard）：并发的多个请求不能同时读到
 * 「未锁定」后各自校验密码，从而绕过 5 次上限。Redis 异常直接抛出（严格失败）：保护失效时不能放行爆破。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayPasswordServiceImpl implements PayPasswordService {

    /** 连续错误上限：6 位数字只有 100 万种组合，5 次足以容忍手误，又让在线穷举不可行 */
    static final int MAX_FAILURES = 5;
    /** 锁定时长，与 60012 的提示文案「30 分钟」一致 */
    static final Duration LOCK_DURATION = Duration.ofMinutes(30);
    static final String FAIL_KEY_PREFIX = "mall:pay-pwd-fail:";

    private final MbrMemberMapper memberMapper;
    private final SmsCodeService smsCodeService;
    private final PasswordEncoder passwordEncoder;
    private final RedisCounter counter;

    @Override
    public SmsCodeVO sendSmsCode(Long memberId, String captchaId, String captchaCode) {
        MbrMember m = require(memberId);
        if (!StringUtils.hasText(m.getMobile())) {
            throw new BusinessException("请先绑定手机号");
        }
        return smsCodeService.send(m.getMobile(), SmsScene.PAY_PASSWORD, captchaId, captchaCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPayPassword(Long memberId, PayPasswordRequest req) {
        String pwd = req.getPayPassword();
        // 先校验密码强度再消费验证码：弱密码被拒后用户可以直接换一个密码重试，不必重新收短信
        checkStrength(pwd);
        MbrMember m = require(memberId);
        if (!StringUtils.hasText(m.getMobile())) {
            throw new BusinessException("请先绑定手机号");
        }
        smsCodeService.verify(m.getMobile(), SmsScene.PAY_PASSWORD, req.getCode());
        memberMapper.update(null, new LambdaUpdateWrapper<MbrMember>()
                .set(MbrMember::getPayPassword, passwordEncoder.encode(pwd))
                .set(MbrMember::getUpdatedBy, memberId)
                .eq(MbrMember::getId, memberId));
        // 本人凭短信重设了密码，之前的错误次数（可能是他人试密码造成的锁定）一并清零
        counter.delete(failKey(memberId));
        // 只记录动作，绝不记录密码明文或密文
        log.info("会员设置支付密码 memberId={}", memberId);
    }

    @Override
    public void verify(Long memberId, String payPassword) {
        MbrMember m = require(memberId);
        if (!StringUtils.hasText(m.getPayPassword())) {
            throw new BusinessException(ResultCode.PAY_PASSWORD_NOT_SET);
        }
        String key = failKey(memberId);
        // 锁定期间不校验密码、不再计数：既不给继续试密码的机会，也不把锁定期越续越长
        if (counter.get(key) >= MAX_FAILURES) {
            throw new BusinessException(ResultCode.PAY_PASSWORD_LOCKED);
        }
        long n = counter.incrementSliding(key, LOCK_DURATION);
        if (n > MAX_FAILURES) {
            // 并发请求抢到了第 6 次及以后的名额：同样视为锁定
            throw new BusinessException(ResultCode.PAY_PASSWORD_LOCKED);
        }
        if (payPassword != null && passwordEncoder.matches(payPassword, m.getPayPassword())) {
            counter.delete(key);
            return;
        }
        long remaining = MAX_FAILURES - n;
        if (remaining <= 0) {
            throw new BusinessException(ResultCode.PAY_PASSWORD_LOCKED);
        }
        throw new BusinessException(ResultCode.PAY_PASSWORD_WRONG, "支付密码错误，还可尝试 " + remaining + " 次");
    }

    /**
     * 6 位数字，且不是 6 位相同数字或逐位 +1 / -1 的连续数字（123456、654321、012345 等）：
     * 这些是最先被猜的组合，5 次尝试机会内就可能命中。
     */
    static void checkStrength(String pwd) {
        if (pwd == null || !pwd.matches("\\d{6}")) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "支付密码为 6 位数字");
        }
        boolean same = true;
        boolean asc = true;
        boolean desc = true;
        for (int i = 1; i < pwd.length(); i++) {
            int d = pwd.charAt(i) - pwd.charAt(i - 1);
            same &= d == 0;
            asc &= d == 1;
            desc &= d == -1;
        }
        if (same || asc || desc) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "支付密码过于简单，请勿使用相同或连续的数字");
        }
    }

    static String failKey(Long memberId) {
        return FAIL_KEY_PREFIX + memberId;
    }

    private MbrMember require(Long memberId) {
        MbrMember m = memberId == null ? null : memberMapper.selectById(memberId);
        if (m == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在");
        }
        return m;
    }
}
