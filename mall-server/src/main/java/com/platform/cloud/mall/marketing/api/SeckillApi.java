package com.platform.cloud.mall.marketing.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀对交易领域开放的能力。
 * <p>
 * 抢购资格在 Redis 里用 Lua 原子扣减活动库存与个人限购（挡住绝大部分流量），
 * 下单成功后交易领域再扣 SKU 真实库存。
 * <p>
 * 事务约定（调用方必须遵守，否则会出现名额重复归还或漏还）：
 * <ul>
 *   <li>{@link #acquire} 自己登记了回滚补偿：调用方事务回滚时，数据库 sold 随事务撤销、Redis 名额由 acquire 内部补回，
 *       调用方<b>不要</b>再在回滚钩子里调用 {@link #release}。</li>
 *   <li>{@link #release} 必须在调用方的业务事务内直接调用（不要放进 afterCommit）：sold 的扣减随该事务提交或回滚，
 *       Redis 名额在该事务提交后才加回。</li>
 * </ul>
 */
public interface SeckillApi {

    /** 活动信息；不存在返回 null */
    SeckillInfo get(Long seckillId);

    /**
     * 抢占名额：活动未开始/已结束/售罄/超出限购时抛业务异常。
     *
     * @param purchased 该会员在本活动已下单（未取消）的数量，由交易领域按订单统计。
     *                  Redis 中的个人已购计数可能因重启 / 淘汰而丢失，以订单为准兜底个人限购
     */
    void acquire(Long seckillId, Long memberId, int quantity, int purchased);

    /** 归还名额（取消、关单、未发货整单退款）；须在调用方业务事务内调用，见类注释 */
    void release(Long seckillId, Long memberId, int quantity);

    record SeckillInfo(Long id, Long goodsId, Long skuId, BigDecimal seckillPrice, Integer stock, Integer sold,
                       Integer limitPerMember, LocalDateTime startAt, LocalDateTime endAt, boolean enabled) {
    }
}
