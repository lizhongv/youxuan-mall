package com.platform.cloud.mall.marketing.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.marketing.entity.Seckill;
import com.platform.cloud.mall.marketing.mapper.SeckillMapper;
import com.platform.cloud.mall.marketing.support.SeckillStockCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 秒杀抢占：时间 / 状态 / 限购校验，Redis 返回码到提示语的映射，DB 兜底失败时归还 Redis 名额。
 */
@ExtendWith(MockitoExtension.class)
class SeckillApiImplTest {

    private static final long ID = 1L;
    private static final long MEMBER = 9L;

    @Mock
    private SeckillMapper seckillMapper;
    @Mock
    private SeckillStockCache stockCache;
    @InjectMocks
    private SeckillApiImpl api;

    private static Seckill activity(LocalDateTime start, LocalDateTime end) {
        Seckill s = new Seckill();
        s.setId(ID);
        s.setGoodsId(10L);
        s.setSkuId(20L);
        s.setSeckillPrice(new BigDecimal("1.00"));
        s.setStock(10);
        s.setSold(0);
        s.setLimitPerMember(2);
        s.setStatus(1);
        s.setStartAt(start);
        s.setEndAt(end);
        return s;
    }

    private static Seckill ongoing() {
        LocalDateTime now = LocalDateTime.now();
        return activity(now.minusHours(1), now.plusHours(1));
    }

    @Test
    void notStarted() {
        LocalDateTime now = LocalDateTime.now();
        when(seckillMapper.selectById(ID)).thenReturn(activity(now.plusHours(1), now.plusHours(2)));
        assertThatThrownBy(() -> api.acquire(ID, MEMBER, 1)).hasMessage("秒杀活动尚未开始");
    }

    @Test
    void ended() {
        LocalDateTime now = LocalDateTime.now();
        when(seckillMapper.selectById(ID)).thenReturn(activity(now.minusHours(2), now.minusHours(1)));
        assertThatThrownBy(() -> api.acquire(ID, MEMBER, 1)).hasMessage("秒杀活动已结束");
    }

    @Test
    void disabled() {
        Seckill s = ongoing();
        s.setStatus(0);
        when(seckillMapper.selectById(ID)).thenReturn(s);
        assertThatThrownBy(() -> api.acquire(ID, MEMBER, 1)).hasMessage("秒杀活动已停止");
    }

    @Test
    void notExists() {
        assertThatThrownBy(() -> api.acquire(ID, MEMBER, 1)).hasMessage("秒杀活动不存在");
    }

    @Test
    void soldOutInDb() {
        Seckill s = ongoing();
        s.setSold(10);
        when(seckillMapper.selectById(ID)).thenReturn(s);
        assertThatThrownBy(() -> api.acquire(ID, MEMBER, 1)).hasMessageContaining("已抢光");
        verify(stockCache, never()).tryAcquire(anyLong(), anyLong(), anyInt(), anyInt());
    }

    @Test
    void singleRequestOverLimit() {
        when(seckillMapper.selectById(ID)).thenReturn(ongoing());
        assertThatThrownBy(() -> api.acquire(ID, MEMBER, 3)).hasMessage("每人限购 2 件");
    }

    @Test
    void invalidQuantity() {
        assertThatThrownBy(() -> api.acquire(ID, MEMBER, 0)).isInstanceOf(BusinessException.class);
    }

    @Test
    void success() {
        when(seckillMapper.selectById(ID)).thenReturn(ongoing());
        when(stockCache.tryAcquire(ID, MEMBER, 1, 2)).thenReturn(9L);
        when(seckillMapper.increaseSold(ID, 1)).thenReturn(1);
        assertThatCode(() -> api.acquire(ID, MEMBER, 1)).doesNotThrowAnyException();
    }

    @Test
    void lazyWarmWhenKeyMissing() {
        when(seckillMapper.selectById(ID)).thenReturn(ongoing());
        when(stockCache.tryAcquire(ID, MEMBER, 1, 2)).thenReturn(SeckillStockCache.NOT_WARMED, 9L);
        when(seckillMapper.increaseSold(ID, 1)).thenReturn(1);
        api.acquire(ID, MEMBER, 1);
        verify(stockCache).warmIfAbsent(anyLong(), anyInt(), any());
    }

    @Test
    void redisSoldOut() {
        when(seckillMapper.selectById(ID)).thenReturn(ongoing());
        when(stockCache.tryAcquire(ID, MEMBER, 1, 2)).thenReturn(SeckillStockCache.SOLD_OUT);
        assertThatThrownBy(() -> api.acquire(ID, MEMBER, 1)).hasMessageContaining("已抢光");
        verify(seckillMapper, never()).increaseSold(anyLong(), anyInt());
    }

    @Test
    void redisInsufficient() {
        when(seckillMapper.selectById(ID)).thenReturn(ongoing());
        when(stockCache.tryAcquire(ID, MEMBER, 2, 2)).thenReturn(SeckillStockCache.INSUFFICIENT);
        assertThatThrownBy(() -> api.acquire(ID, MEMBER, 2)).hasMessage("秒杀剩余数量不足");
    }

    @Test
    void redisOverPersonalLimit() {
        when(seckillMapper.selectById(ID)).thenReturn(ongoing());
        when(stockCache.tryAcquire(ID, MEMBER, 1, 2)).thenReturn(SeckillStockCache.OVER_LIMIT);
        assertThatThrownBy(() -> api.acquire(ID, MEMBER, 1)).hasMessageContaining("每人限购 2 件");
    }

    @Test
    void releaseOnlyWhenDbSoldDecreased() {
        when(seckillMapper.decreaseSold(ID, 1)).thenReturn(0);
        api.release(ID, MEMBER, 1);
        verify(stockCache, never()).release(anyLong(), anyLong(), anyInt());

        when(seckillMapper.decreaseSold(ID, 1)).thenReturn(1);
        api.release(ID, MEMBER, 1);
        // 无事务时 afterCommit 立即执行
        verify(stockCache).release(ID, MEMBER, 1);
    }

    @Test
    void remainingNeverNegative() {
        Seckill s = ongoing();
        s.setSold(12);
        org.assertj.core.api.Assertions.assertThat(SeckillServiceImpl.remaining(s)).isZero();
        s.setSold(3);
        org.assertj.core.api.Assertions.assertThat(SeckillServiceImpl.remaining(s)).isEqualTo(7);
    }
}
