package com.platform.cloud.bootstrap.common.idempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 幂等控制注解。
 * <p>
 * 客户端必须在请求头里带 {@code X-Idempotent-Key}（或自定义 keyHeader），值为唯一字符串（推荐 UUID）。
 * 同一 key 在 {@link #ttl()} 时间内只会被执行一次，再次请求直接返回 {@link com.platform.cloud.bootstrap.common.web.ResultCode#DUPLICATE_REQUEST}。
 * <p>
 * 实现：基于 Redis SETNX，AOP 切面见 {@link IdempotentAspect}。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 业务前缀，避免不同接口共用 key 冲突 */
    String prefix() default "idem:";

    /** 客户端传 key 的请求头名 */
    String keyHeader() default "X-Idempotent-Key";

    /** key 的存活时间 */
    long ttl() default 60;

    TimeUnit unit() default TimeUnit.SECONDS;
}
