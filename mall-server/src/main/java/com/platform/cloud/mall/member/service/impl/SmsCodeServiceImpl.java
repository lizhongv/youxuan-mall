package com.platform.cloud.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.member.config.SmsProperties;
import com.platform.cloud.mall.member.entity.MbrMember;
import com.platform.cloud.mall.member.mapper.MbrMemberMapper;
import com.platform.cloud.mall.member.service.SmsCodeService;
import com.platform.cloud.mall.member.support.SmsScene;
import com.platform.cloud.mall.member.support.SmsSender;
import com.platform.cloud.mall.member.vo.SmsCodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 短信验证码，状态全部放 Redis（多实例共享、自动过期）。
 * <ul>
 *   <li>code:{scene}:{mobile} 验证码，5 分钟</li>
 *   <li>fail:{scene}:{mobile} 错误次数，与验证码同寿命，达到 5 次连同验证码一起删除</li>
 *   <li>interval:{mobile} 发送间隔锁，60 秒，按手机号而非场景，防止换场景绕过</li>
 *   <li>daily:{yyyyMMdd}:{mobile} 当日发送次数</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeServiceImpl implements SmsCodeService {

    static final int CODE_LENGTH = 6;
    static final Duration CODE_TTL = Duration.ofMinutes(5);
    static final Duration SEND_INTERVAL = Duration.ofSeconds(60);
    static final int MAX_VERIFY_FAILURES = 5;

    private static final String PREFIX = "mall:sms:";
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final MbrMemberMapper memberMapper;
    private final SmsSender smsSender;
    private final SmsProperties props;

    @Override
    public SmsCodeVO send(String mobile, SmsScene scene) {
        boolean registered = isRegistered(mobile);
        if (scene == SmsScene.REGISTER && registered) {
            throw new BusinessException("该手机号已注册，请直接登录");
        }
        if (scene == SmsScene.RESET && !registered) {
            throw new BusinessException("该手机号未注册");
        }

        String intervalKey = PREFIX + "interval:" + mobile;
        Boolean first = redis.opsForValue().setIfAbsent(intervalKey, "1", SEND_INTERVAL);
        if (!Boolean.TRUE.equals(first)) {
            throw new BusinessException("验证码发送过于频繁，请 " + SEND_INTERVAL.toSeconds() + " 秒后再试");
        }
        String dailyKey = PREFIX + "daily:" + LocalDate.now().format(DAY) + ":" + mobile;
        Long today = redis.opsForValue().increment(dailyKey);
        if (today != null && today == 1L) {
            redis.expire(dailyKey, Duration.ofDays(1));
        }
        if (today != null && today > props.getDailyLimit()) {
            throw new BusinessException("今日验证码发送次数已达上限，请明天再试");
        }

        String code = generateCode();
        String codeKey = codeKey(scene, mobile);
        redis.opsForValue().set(codeKey, code, CODE_TTL);
        // 新验证码重新计算错误次数
        redis.delete(failKey(scene, mobile));
        try {
            smsSender.sendCode(mobile, scene, code);
        } catch (RuntimeException e) {
            // 发送失败时撤销验证码与间隔锁，让用户可以马上重试
            redis.delete(codeKey);
            redis.delete(intervalKey);
            log.error("短信发送失败 scene={}", scene, e);
            throw new BusinessException("短信发送失败，请稍后重试");
        }
        return new SmsCodeVO(props.isDevMode() ? code : null);
    }

    @Override
    public void verify(String mobile, SmsScene scene, String code) {
        String codeKey = codeKey(scene, mobile);
        String stored = redis.opsForValue().get(codeKey);
        if (stored == null) {
            throw new BusinessException("验证码已失效，请重新获取");
        }
        if (code == null || !constantTimeEquals(stored, code.trim())) {
            String failKey = failKey(scene, mobile);
            Long failures = redis.opsForValue().increment(failKey);
            redis.expire(failKey, CODE_TTL);
            if (failures != null && failures >= MAX_VERIFY_FAILURES) {
                // 作废：6 位验证码有 100 万种组合，限制 5 次让暴力猜中的概率可以忽略
                redis.delete(codeKey);
                redis.delete(failKey);
                throw new BusinessException("验证码错误次数过多，请重新获取");
            }
            throw new BusinessException("验证码错误");
        }
        redis.delete(codeKey);
        redis.delete(failKey(scene, mobile));
    }

    private boolean isRegistered(String mobile) {
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<MbrMember>().eq(MbrMember::getMobile, mobile));
        return count != null && count > 0;
    }

    static String generateCode() {
        int bound = (int) Math.pow(10, CODE_LENGTH);
        return String.format("%0" + CODE_LENGTH + "d", RANDOM.nextInt(bound));
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String codeKey(SmsScene scene, String mobile) {
        return PREFIX + "code:" + scene.name() + ":" + mobile;
    }

    private static String failKey(SmsScene scene, String mobile) {
        return PREFIX + "fail:" + scene.name() + ":" + mobile;
    }
}
