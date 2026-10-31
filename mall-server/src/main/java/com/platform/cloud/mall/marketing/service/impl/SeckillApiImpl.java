package com.platform.cloud.mall.marketing.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.marketing.api.SeckillApi;
import com.platform.cloud.mall.marketing.entity.Seckill;
import com.platform.cloud.mall.marketing.mapper.SeckillMapper;
import com.platform.cloud.mall.marketing.support.SeckillState;
import com.platform.cloud.mall.marketing.support.SeckillStockCache;
import com.platform.cloud.mall.marketing.support.TxCallbacks;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 秒杀名额：Redis Lua 原子挡流 + 数据库 sold 条件更新兜底。
 * <p>
 * Redis 不参与数据库事务，因此：抢占成功后若调用方事务回滚，自动把 Redis 名额补回；
 * 归还时 Redis 侧在事务提交后再加回，避免事务回滚了名额却已经放出去。
 */
@Service
@RequiredArgsConstructor
public class SeckillApiImpl implements SeckillApi {

    private final SeckillMapper seckillMapper;
    private final SeckillStockCache stockCache;

    @Override
    public SeckillInfo get(Long seckillId) {
        Seckill s = seckillMapper.selectById(seckillId);
        if (s == null) {
            return null;
        }
        return new SeckillInfo(s.getId(), s.getGoodsId(), s.getSkuId(), s.getSeckillPrice(), s.getStock(), s.getSold(),
                s.getLimitPerMember(), s.getStartAt(), s.getEndAt(), Objects.equals(s.getStatus(), 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acquire(Long seckillId, Long memberId, int quantity, int purchased) {
        if (quantity <= 0) {
            throw new BusinessException("购买数量必须大于 0");
        }
        Seckill s = seckillMapper.selectById(seckillId);
        checkAvailable(s, quantity, LocalDateTime.now());
        int limit = s.getLimitPerMember() == null ? 1 : s.getLimitPerMember();
        // 以订单为准的个人限购兜底：Redis 个人已购 Hash 丢失后 Lua 会认为「从未买过」，这里先挡住
        if (Math.max(purchased, 0) + quantity > limit) {
            throw new BusinessException("每人限购 " + limit + " 件，您已达到限购数量");
        }

        long r = stockCache.tryAcquire(seckillId, memberId, quantity, limit);
        if (r == SeckillStockCache.NOT_WARMED) {
            // 预热丢失（Redis 重启 / 键过期）：以数据库为准懒加载一次再试
            stockCache.warmIfAbsent(seckillId, SeckillServiceImpl.remaining(s), s.getEndAt());
            r = stockCache.tryAcquire(seckillId, memberId, quantity, limit);
        }
        if (r == SeckillStockCache.NOT_WARMED) {
            throw new BusinessException("活动太火爆，请稍后再试");
        }
        if (r == SeckillStockCache.SOLD_OUT) {
            throw new BusinessException("手慢了，秒杀商品已抢光");
        }
        if (r == SeckillStockCache.INSUFFICIENT) {
            throw new BusinessException("秒杀剩余数量不足");
        }
        if (r == SeckillStockCache.OVER_LIMIT) {
            throw new BusinessException("每人限购 " + limit + " 件，您已达到限购数量");
        }
        // 从这里开始 Redis 已扣，任何失败（含调用方后续步骤回滚）都要补回
        TxCallbacks.afterRollback(() -> stockCache.release(seckillId, memberId, quantity));
        if (seckillMapper.increaseSold(seckillId, quantity) == 0) {
            throw new BusinessException("手慢了，秒杀商品已抢光");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(Long seckillId, Long memberId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        // 在调用方事务内扣 sold：取消事务回滚时 sold 一起回滚；
        // sold >= quantity 条件保证不会减成负数；影响 0 行说明已归还过，Redis 也不再加回，保持幂等。
        // Redis 名额登记在调用方事务的 afterCommit 上，事务提交后恰好加回一次
        if (seckillMapper.decreaseSold(seckillId, quantity) > 0) {
            TxCallbacks.afterCommit(() -> stockCache.release(seckillId, memberId, quantity));
        }
    }

    /** 活动可抢校验：存在、启用、在时间内、单次不超过限购 */
    static void checkAvailable(Seckill s, int quantity, LocalDateTime now) {
        if (s == null) {
            throw new BusinessException("秒杀活动不存在");
        }
        if (!Objects.equals(s.getStatus(), 1)) {
            throw new BusinessException("秒杀活动已停止");
        }
        SeckillState state = SeckillState.of(s.getStartAt(), s.getEndAt(), now);
        if (state == SeckillState.NOT_STARTED) {
            throw new BusinessException("秒杀活动尚未开始");
        }
        if (state == SeckillState.ENDED) {
            throw new BusinessException("秒杀活动已结束");
        }
        int sold = s.getSold() == null ? 0 : s.getSold();
        if (s.getStock() != null && sold >= s.getStock()) {
            throw new BusinessException("手慢了，秒杀商品已抢光");
        }
        int limit = s.getLimitPerMember() == null ? 1 : s.getLimitPerMember();
        if (quantity > limit) {
            throw new BusinessException("每人限购 " + limit + " 件");
        }
    }
}
