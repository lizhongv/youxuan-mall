package com.platform.cloud.mall.marketing.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀对交易领域开放的能力。
 * <p>
 * 抢购资格在 Redis 里用 Lua 原子扣减活动库存与个人限购（挡住绝大部分流量），
 * 下单成功后交易领域再扣 SKU 真实库存；下单失败或关单时调用 {@link #release} 归还名额。
 */
public interface SeckillApi {

    /** 活动信息；不存在返回 null */
    SeckillInfo get(Long seckillId);

    /** 抢占名额：活动未开始/已结束/售罄/超出限购时抛业务异常 */
    void acquire(Long seckillId, Long memberId, int quantity);

    /** 归还名额 */
    void release(Long seckillId, Long memberId, int quantity);

    record SeckillInfo(Long id, Long goodsId, Long skuId, BigDecimal seckillPrice, Integer stock, Integer sold,
                       Integer limitPerMember, LocalDateTime startAt, LocalDateTime endAt, boolean enabled) {
    }
}
