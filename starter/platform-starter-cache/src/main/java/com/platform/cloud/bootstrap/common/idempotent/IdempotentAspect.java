package com.platform.cloud.bootstrap.common.idempotent;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final StringRedisTemplate redis;

    @Around("@annotation(idem)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idem) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return pjp.proceed();
        }
        HttpServletRequest req = attrs.getRequest();
        String key = req.getHeader(idem.keyHeader());
        if (!StringUtils.hasText(key)) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "缺少幂等头 " + idem.keyHeader());
        }
        String redisKey = idem.prefix() + key;
        Boolean firstTime;
        try {
            firstTime = redis.opsForValue().setIfAbsent(redisKey, "1", idem.ttl(), idem.unit());
        } catch (RuntimeException e) {
            // Redis 不可用时直接执行业务：幂等令牌只是第一道防线，下单 / 支付等关键写操作
            // 还有数据库唯一约束与状态条件更新兜底，宁可失去这层去重也不让整个下单链路不可用
            log.warn("幂等检查失败（Redis 不可用），直接执行业务 key={}: {}", redisKey, e.getMessage());
            return pjp.proceed();
        }
        if (firstTime == null || !firstTime) {
            log.warn("Duplicate request blocked, key={}", redisKey);
            throw new BusinessException(ResultCode.DUPLICATE_REQUEST);
        }
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            // 失败时删除 key，让客户端可以用同一个 key 重试
            releaseQuietly(redisKey);
            throw t;
        }
    }

    /** 删除失败不能覆盖业务异常本身；key 到期后自然释放 */
    private void releaseQuietly(String redisKey) {
        try {
            redis.delete(redisKey);
        } catch (RuntimeException e) {
            log.warn("释放幂等 key 失败 key={}: {}", redisKey, e.getMessage());
        }
    }
}
