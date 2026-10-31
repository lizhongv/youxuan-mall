package com.platform.cloud.mall.member.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.mall.common.security.CaptchaService;
import com.platform.cloud.mall.common.security.RedisCounter;
import com.platform.cloud.mall.common.util.RequestUtils;
import com.platform.cloud.mall.member.config.SmsProperties;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 短信验证码，状态全部放 Redis（多实例共享、自动过期）。
 * <ul>
 *   <li>code:{scene}:{mobile} 验证码，5 分钟</li>
 *   <li>fail:{scene}:{mobile} 错误次数，与验证码同寿命，达到 5 次连同验证码一起删除</li>
 *   <li>interval:{mobile} 发送间隔锁，60 秒，按手机号而非场景，防止换场景绕过</li>
 *   <li>daily:{yyyyMMdd}:{mobile} 当日发送次数</li>
 *   <li>ip:{ip} 每小时发送次数；global:{yyyyMMddHHmm} 全站每分钟发送次数（防刷短信费）</li>
 * </ul>
 * Redis 异常直接抛出（严格失败）：频控失效时宁可暂停发送，也不能让短信费失控。
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
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final RedisCounter counter;
    private final CaptchaService captchaService;
    private final SmsSender smsSender;
    private final SmsProperties props;

    @Override
    public SmsCodeVO send(String mobile, SmsScene scene, String captchaId, String captchaCode) {
        // 1. 图形验证码：一次性，先于一切计数，避免没过验证码的请求消耗正常用户的频控额度
        if (!captchaService.verify(CaptchaService.SCOPE_APP, captchaId, captchaCode)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "图形验证码错误或已过期，请刷新后重试");
        }
        // 2. 全站每分钟上限（费用保险丝）
        String globalKey = PREFIX + "global:" + LocalDateTime.now().format(MINUTE);
        if (counter.incrementInWindow(globalKey, Duration.ofMinutes(2)) > props.getGlobalPerMinuteLimit()) {
            log.warn("短信全站发送量达到每分钟上限 {}", props.getGlobalPerMinuteLimit());
            throw new BusinessException(ResultCode.DUPLICATE_REQUEST, "短信服务繁忙，请稍后再试");
        }
        // 3. 同一 IP 每小时上限
        String ip = RequestUtils.clientIp();
        if (counter.incrementInWindow(PREFIX + "ip:" + ip, Duration.ofHours(1)) > props.getIpHourlyLimit()) {
            throw new BusinessException(ResultCode.DUPLICATE_REQUEST, "验证码请求过于频繁，请稍后再试");
        }
        // 4. 同一手机号 60 秒间隔
        String intervalKey = PREFIX + "interval:" + mobile;
        Boolean first = redis.opsForValue().setIfAbsent(intervalKey, "1", SEND_INTERVAL);
        if (!Boolean.TRUE.equals(first)) {
            throw new BusinessException("验证码发送过于频繁，请 " + SEND_INTERVAL.toSeconds() + " 秒后再试");
        }
        // 5. 同一手机号每日上限
        String dailyKey = PREFIX + "daily:" + LocalDate.now().format(DAY) + ":" + mobile;
        if (counter.incrementInWindow(dailyKey, Duration.ofDays(1)) > props.getDailyLimit()) {
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
            // 发送失败时撤销验证码与间隔锁，让用户可以马上重试（图形验证码已消费，需要重新获取）
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
            long failures = counter.incrementInWindow(failKey, CODE_TTL);
            if (failures >= MAX_VERIFY_FAILURES) {
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
