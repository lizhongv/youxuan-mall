package com.platform.cloud.mall.common.security;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.mall.common.config.MallProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * 密码登录防爆破（管理端与商城端共用，按场景隔离计数）。
 * <ol>
 *   <li><b>IP 限流</b>：同一 IP 每分钟最多 {@code mall.security.login-ip-per-minute} 次，挡住单机脚本。</li>
 *   <li><b>失败锁定按「账号 + IP」计数</b>：连续失败 {@value #MAX_FAILURES} 次锁 15 分钟。只锁这个 IP，
 *       攻击者无法通过故意输错把真正的管理员锁在系统外。</li>
 *   <li><b>图形验证码</b>：该账号在本 IP 已失败 {@value #CAPTCHA_AFTER_FAILURES} 次，或在所有 IP 上累计失败
 *       {@value #ACCOUNT_CAPTCHA_AFTER_FAILURES} 次（分布式爆破）后，必须带验证码。</li>
 *   <li><b>原子预占</b>：校验密码前先用 INCR 预占一次尝试，判断用的是 INCR 的返回值。若先读计数再校验，
 *       并发的 N 个请求会同时读到「未锁定」，锁定形同虚设。登录成功再清零。</li>
 * </ol>
 * Redis 异常直接抛出（严格失败，返回 503）：登录保护失效时不能放行爆破。
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptGuard {

    /** 连续失败上限：5 次足以容忍正常输错，又能把单 IP 的在线爆破压到每小时 20 次以内 */
    public static final int MAX_FAILURES = 5;
    /** 失败 2 次后要验证码：第一次输错多半是手误，不打扰正常用户 */
    public static final int CAPTCHA_AFTER_FAILURES = 2;
    /** 账号在所有 IP 上的累计失败阈值，超过后任何 IP 都要验证码 */
    public static final int ACCOUNT_CAPTCHA_AFTER_FAILURES = 10;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    static final Duration RATE_WINDOW = Duration.ofMinutes(1);

    private static final String PREFIX = "mall:login:";

    private final RedisCounter counter;
    private final CaptchaService captchaService;
    private final MallProperties props;

    /** 登录场景：决定计数 key 前缀、验证码作用域与提示文案 */
    public enum Scene {
        ADMIN("admin", CaptchaService.SCOPE_ADMIN, "用户名或密码错误", ""),
        MEMBER("member", CaptchaService.SCOPE_APP, "手机号或密码错误", "，可使用验证码登录");

        final String key;
        final String captchaScope;
        final String wrongMsg;
        final String lockHint;

        Scene(String key, String captchaScope, String wrongMsg, String lockHint) {
            this.key = key;
            this.captchaScope = captchaScope;
            this.wrongMsg = wrongMsg;
            this.lockHint = lockHint;
        }
    }

    /**
     * 一次登录尝试的上下文。
     *
     * @param priorFailures 本次之前该账号在本 IP 的连续失败次数
     */
    public record Attempt(Scene scene, String principal, String ip, long priorFailures, boolean captchaRequired) {
    }

    /**
     * 开始一次尝试：限流 → 预占计数 → 锁定判断 → 验证码校验。全部通过后调用方再校验密码。
     *
     * @throws BusinessException 42900 限流；60000 已锁定；40002 + {needCaptcha:true} 需要 / 验证码错误
     */
    public Attempt begin(Scene scene, String principal, String ip, String captchaId, String captchaCode) {
        String p = normalize(principal);
        long perMinute = counter.incrementInWindow(rateKey(scene, ip), RATE_WINDOW);
        if (perMinute > props.getSecurity().getLoginIpPerMinute()) {
            throw new BusinessException(ResultCode.DUPLICATE_REQUEST, "登录尝试过于频繁，请稍后再试");
        }
        long n = counter.incrementSliding(failKey(scene, p, ip), LOCK_DURATION);
        long prior = n - 1;
        if (prior >= MAX_FAILURES) {
            throw new BusinessException("登录失败次数过多，已锁定，请 " + LOCK_DURATION.toMinutes() + " 分钟后再试"
                    + scene.lockHint);
        }
        boolean captchaRequired = prior >= CAPTCHA_AFTER_FAILURES
                || counter.get(accountKey(scene, p)) >= ACCOUNT_CAPTCHA_AFTER_FAILURES;
        Attempt attempt = new Attempt(scene, p, ip, prior, captchaRequired);
        if (captchaRequired && !captchaService.verify(scene.captchaScope, captchaId, captchaCode)) {
            // 验证码不对不算一次密码尝试：撤销预占，避免用户因为看不清验证码被锁
            counter.decrement(failKey(scene, p, ip));
            String msg = StringUtils.hasText(captchaCode) ? "图形验证码错误或已过期" : "请输入图形验证码";
            throw new BusinessException(ResultCode.PARAM_MISSING, msg, needCaptcha(true));
        }
        return attempt;
    }

    /**
     * 密码错误：计数已在 begin 中预占，这里累计账号维度失败次数并抛出给用户的提示。
     * 提示里带 needCaptcha，前端据此在下一次登录时显示验证码。
     */
    public BusinessException onFailure(Attempt a) {
        counter.incrementSliding(accountKey(a.scene(), a.principal()), LOCK_DURATION);
        long failures = a.priorFailures() + 1;
        boolean nextNeedsCaptcha = failures >= CAPTCHA_AFTER_FAILURES || a.captchaRequired();
        String msg;
        if (failures >= MAX_FAILURES) {
            msg = a.scene().wrongMsg + "次数过多，已锁定 " + LOCK_DURATION.toMinutes() + " 分钟" + a.scene().lockHint;
        } else {
            long remaining = MAX_FAILURES - failures;
            msg = remaining <= 2 ? a.scene().wrongMsg + "，再错 " + remaining + " 次将锁定" : a.scene().wrongMsg;
        }
        return new BusinessException(ResultCode.BUSINESS_ERROR, msg, needCaptcha(nextNeedsCaptcha));
    }

    /** 密码不对之外的原因（如账号停用）拒绝登录时调用：不算失败，归还预占 */
    public void release(Attempt a) {
        counter.decrement(failKey(a.scene(), a.principal(), a.ip()));
    }

    /** 登录成功：清掉本 IP 的连续失败与账号累计失败 */
    public void onSuccess(Attempt a) {
        reset(a.scene(), a.principal(), a.ip());
    }

    /** 通过其他方式证明了身份（验证码登录、重置密码）后解除锁定 */
    public void reset(Scene scene, String principal, String ip) {
        String p = normalize(principal);
        counter.delete(failKey(scene, p, ip));
        counter.delete(accountKey(scene, p));
    }

    static Map<String, Object> needCaptcha(boolean need) {
        return Map.of("needCaptcha", need);
    }

    /** 用户名大小写不敏感地计数，防止通过变换大小写绕过锁定 */
    private static String normalize(String principal) {
        return principal == null ? "" : principal.trim().toLowerCase(Locale.ROOT);
    }

    static String rateKey(Scene scene, String ip) {
        return PREFIX + "rate:" + scene.key + ":" + ip;
    }

    static String failKey(Scene scene, String principal, String ip) {
        return PREFIX + "fail:" + scene.key + ":" + principal + ":" + ip;
    }

    static String accountKey(Scene scene, String principal) {
        return PREFIX + "fail:" + scene.key + ":" + principal;
    }
}
