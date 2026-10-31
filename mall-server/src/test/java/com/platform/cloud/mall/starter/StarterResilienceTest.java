package com.platform.cloud.mall.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cloud.bootstrap.common.cache.CacheInvalidator;
import com.platform.cloud.bootstrap.common.cache.LoggingCacheErrorHandler;
import com.platform.cloud.bootstrap.common.cache.MultiLevelCache;
import com.platform.cloud.bootstrap.common.idempotent.Idempotent;
import com.platform.cloud.bootstrap.common.idempotent.IdempotentAspect;
import com.platform.cloud.bootstrap.common.security.TokenBlacklist;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Redis 不可用时的降级：缓存回源、幂等切面直接执行、refresh 一次性标记原子。
 */
class StarterResilienceTest {

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void multiLevelCacheTreatsRedisFailureAsMiss() {
        Cache l1 = new ConcurrentMapCache("l1");
        Cache l2 = mock(Cache.class);
        when(l2.get(any())).thenThrow(new RedisConnectionFailureException("down"));
        doThrow(new RedisConnectionFailureException("down")).when(l2).put(any(), any());
        doThrow(new RedisConnectionFailureException("down")).when(l2).evict(any());
        MultiLevelCache cache = new MultiLevelCache("home", l1, l2, mock(CacheInvalidator.class));

        assertThat(cache.get("k")).isNull();
        assertThat(cache.get("k", () -> "loaded")).isEqualTo("loaded");
        // L1 仍然可用
        assertThat(cache.get("k").get()).isEqualTo("loaded");
        assertThatCode(() -> cache.evict("k")).doesNotThrowAnyException();
    }

    @Test
    void cacheErrorHandlerSwallowsErrors() {
        LoggingCacheErrorHandler h = new LoggingCacheErrorHandler();
        Cache c = new ConcurrentMapCache("c");
        RuntimeException e = new RedisConnectionFailureException("down");
        assertThatCode(() -> {
            h.handleCacheGetError(e, c, "k");
            h.handleCachePutError(e, c, "k", "v");
            h.handleCacheEvictError(e, c, "k");
            h.handleCacheClearError(e, c);
        }).doesNotThrowAnyException();
    }

    static class Target {
        @Idempotent
        public void submit() {
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void idempotentAspectProceedsWhenRedisIsDown() throws Throwable {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenThrow(new RedisConnectionFailureException("down"));
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Idempotent-Key", "k1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenReturn("done");
        Method m = Target.class.getMethod("submit");

        Object result = new IdempotentAspect(redis).around(pjp, m.getAnnotation(Idempotent.class));

        assertThat(result).isEqualTo("done");
    }

    @Test
    @SuppressWarnings("unchecked")
    void refreshTokenConsumeIsSetNx() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(eq("auth:bl:j1"), eq("1"), any(Duration.class))).thenReturn(true, false);
        TokenBlacklist bl = new TokenBlacklist(redis);

        assertThat(bl.tryConsume("j1", Duration.ofMinutes(5))).isTrue();
        assertThat(bl.tryConsume("j1", Duration.ofMinutes(5))).isFalse();
        assertThat(bl.tryConsume("j1", Duration.ZERO)).isFalse();
        assertThat(bl.tryConsume(null, Duration.ofMinutes(5))).isFalse();
    }

    @Test
    void redisCacheTypingRejectsGadgetClasses() throws Exception {
        // 与 CacheConfig 使用同一校验器：白名单外的类不能被反序列化。
        // java.awt.Point 不在 Jackson 内置黑名单里，只有我们的白名单能挡住它，用来证明校验器生效
        String outside = "{\"@class\":\"java.awt.Point\",\"x\":1,\"y\":2}";
        Method m = Class.forName("com.platform.cloud.bootstrap.common.cache.CacheConfig")
                .getDeclaredMethod("typeValidator");
        m.setAccessible(true);
        ObjectMapper om = new ObjectMapper();
        om.activateDefaultTyping((com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator) m.invoke(null),
                ObjectMapper.DefaultTyping.NON_FINAL, com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);

        String evil = "{\"@class\":\"javax.swing.JEditorPane\",\"page\":\"http://evil/\"}";
        assertThat(catchAny(() -> om.readValue(evil, Object.class))).isNotNull();
        assertThat(catchAny(() -> om.readValue(outside, Object.class))).isNotNull();

        String ok = "[\"java.util.ArrayList\",[\"a\"]]";
        assertThat(om.readValue(ok, Object.class)).isInstanceOf(java.util.ArrayList.class);
    }

    private static Throwable catchAny(ThrowingRunnable r) {
        try {
            r.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }
}
