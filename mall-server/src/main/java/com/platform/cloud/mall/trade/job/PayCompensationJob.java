package com.platform.cloud.mall.trade.job;

import com.platform.cloud.mall.trade.pay.service.PaySettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 支付补偿（每分钟，分布式锁）：回调可能丢失（网络、发布重启），不能只靠回调落账。
 * <ul>
 *   <li>5 分钟前发起仍 WAITING 的微信 / 支付宝流水主动查单：已付 → 落账或「关单后到账」自动退款；
 *       订单已关闭仍未付 → 关闭渠道订单；</li>
 *   <li>退款中超过 5 分钟的售后单查询渠道退款结果。</li>
 * </ul>
 * 与关单任务错开在第 45 秒执行，减少与关单的并发（并发本身也是安全的，均为条件更新）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayCompensationJob {

    /** 每轮最多处理条数：每条都是一次渠道网络调用，限制单轮耗时在一分钟内 */
    static final int BATCH = 50;
    static final String JOB = "pay-compensate";

    private final PaySettlementService settlement;
    private final TradeJobLock lock;

    @Scheduled(cron = "45 * * * * ?")
    public void compensate() {
        if (!lock.tryLock(JOB)) {
            return;
        }
        int paid = settlement.compensateWaiting(BATCH);
        int refunds = settlement.syncRefunding(BATCH);
        if (paid > 0 || refunds > 0) {
            log.info("{} 本轮查单 {} 条，查退款 {} 条", JOB, paid, refunds);
        }
    }
}
