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
        Boolean firstTime = redis.opsForValue().setIfAbsent(redisKey, "1", idem.ttl(), idem.unit());
        if (firstTime == null || !firstTime) {
            log.warn("Duplicate request blocked, key={}", redisKey);
            throw new BusinessException(ResultCode.DUPLICATE_REQUEST);
        }
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            // 失败时删除 key，让客户端可以用同一个 key 重试
            redis.delete(redisKey);
            throw t;
        }
    }
}
