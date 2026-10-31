package com.platform.cloud.mall.common.security;

import com.platform.cloud.bootstrap.common.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 令牌版本号：缓存命中 / 回源 / 作废后删缓存；校验器按 utype 区分用户。
 */
class TokenVersionStoreTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> ops;
    private TokenVersionSource memberSource;
    private TokenVersionStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        memberSource = mock(TokenVersionSource.class);
        when(memberSource.userType()).thenReturn(UserType.MEMBER);
        store = new TokenVersionStore(redis, List.of(memberSource));
    }

    private static Claims claims(String utype, String sub, Integer tv) {
        var b = Jwts.claims().subject(sub);
        if (utype != null) {
            b.add("utype", utype);
        }
        if (tv != null) {
            b.add(JwtService.CLAIM_TOKEN_VERSION, tv);
        }
        return b.build();
    }

    @Test
    void cachedVersionIsUsedWithoutDatabase() {
        when(ops.get("mall:tv:member:7")).thenReturn("3");

        assertThat(store.current(UserType.MEMBER, 7L)).isEqualTo(3);
        verify(memberSource, never()).loadVersion(7L);
    }

    @Test
    void cacheMissLoadsFromDatabaseAndCaches() {
        when(memberSource.loadVersion(7L)).thenReturn(2);

        assertThat(store.current(UserType.MEMBER, 7L)).isEqualTo(2);
        verify(ops).set("mall:tv:member:7", "2", TokenVersionStore.CACHE_TTL);
    }

    @Test
    void deletedUserNeverMatches() {
        when(memberSource.loadVersion(7L)).thenReturn(null);

        assertThat(store.matches(UserType.MEMBER, 7L, claims("MEMBER", "7", 0))).isFalse();
    }

    @Test
    void tokenWithoutVersionClaimIsTreatedAsZero() {
        when(ops.get("mall:tv:member:7")).thenReturn("0");
        assertThat(store.matches(UserType.MEMBER, 7L, claims("MEMBER", "7", null))).isTrue();

        when(ops.get("mall:tv:member:7")).thenReturn("1");
        assertThat(store.matches(UserType.MEMBER, 7L, claims("MEMBER", "7", null))).isFalse();
    }

    @Test
    void revokeAllIncrementsAndEvictsCache() {
        store.revokeAll(UserType.MEMBER, 7L);

        verify(memberSource).increment(7L);
        verify(redis).delete("mall:tv:member:7");
    }

    @Test
    void cacheEvictFailureDoesNotFailBusinessOperation() {
        doThrow(new RedisConnectionFailureException("down")).when(redis).delete(anyString());

        store.revokeAll(UserType.MEMBER, 7L);
        verify(memberSource).increment(7L);
    }

    @Test
    void redisReadFailurePropagatesToCaller() {
        when(ops.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        assertThatThrownBy(() -> store.current(UserType.MEMBER, 7L)).isInstanceOf(RedisConnectionFailureException.class);
    }

    @Test
    void checkerUsesUtypeAndSubject() {
        MallTokenVersionChecker checker = new MallTokenVersionChecker(store);
        when(ops.get("mall:tv:member:7")).thenReturn("2");

        assertThat(checker.isCurrent(claims("MEMBER", "7", 2))).isTrue();
        assertThat(checker.isCurrent(claims("MEMBER", "7", 1))).isFalse();
        assertThat(checker.isCurrent(claims(null, "7", 2))).isFalse();
        assertThat(checker.isCurrent(claims("MEMBER", "abc", 2))).isFalse();
    }
}
