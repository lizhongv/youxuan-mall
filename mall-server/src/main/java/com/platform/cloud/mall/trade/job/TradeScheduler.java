package com.platform.cloud.mall.trade.job;

import com.platform.cloud.mall.trade.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * 交易定时任务（每分钟）：关闭超时未支付订单、自动确认收货。
 * <p>
 * 每个订单单独一个事务（调用 OrderService 的事务方法），某个订单失败只记日志，不影响同批其他订单；
 * 按订单 id 游标分页，每页 {@link #BATCH} 条，取完或达到单轮上限 {@link #MAX_PER_ROUND} 为止，
 * 积压时下一分钟继续。游标保证失败（或被跳过）的订单不会一直占着队头、让后面的订单永远处理不到。
 * 过期优惠券置为 EXPIRED 属于营销领域的任务，不在这里处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeScheduler {

    static final int BATCH = 200;
    /** 单轮上限：2000 单约十几秒，留足一分钟调度间隔，避免两轮重叠 */
    static final int MAX_PER_ROUND = 2000;
    static final String JOB_CLOSE = "close-expired-order";
    static final String JOB_RECEIVE = "auto-receive-order";

    private final OrderService orderService;
    private final TradeJobLock lock;

    @Scheduled(cron = "0 * * * * ?")
    public void closeExpiredOrders() {
        if (!lock.tryLock(JOB_CLOSE)) {
            return;
        }
        process(JOB_CLOSE, orderService::findExpiredUnpaid, orderService::closeExpired);
    }

    @Scheduled(cron = "30 * * * * ?")
    public void autoReceiveOrders() {
        if (!lock.tryLock(JOB_RECEIVE)) {
            return;
        }
        process(JOB_RECEIVE, orderService::findAutoReceivable, orderService::autoReceive);
    }

    /** 按 id 游标翻页：fetcher(afterId, limit) 返回 id 升序的一页 */
    void process(String job, BiFunction<Long, Integer, List<Long>> fetcher, Consumer<Long> action) {
        int total = 0;
        int ok = 0;
        Long cursor = null;
        while (total < MAX_PER_ROUND) {
            List<Long> ids;
            try {
                ids = fetcher.apply(cursor, Math.min(BATCH, MAX_PER_ROUND - total));
            } catch (RuntimeException e) {
                log.warn("{} 查询待处理订单失败", job, e);
                break;
            }
            if (ids.isEmpty()) {
                break;
            }
            for (Long id : ids) {
                try {
                    action.accept(id);
                    ok++;
                } catch (RuntimeException e) {
                    log.warn("{} 处理订单失败 orderId={}", job, id, e);
                }
            }
            total += ids.size();
            cursor = ids.get(ids.size() - 1);
            if (ids.size() < BATCH) {
                break;
            }
        }
        if (total > 0) {
            log.info("{} 本轮处理 {} 条，成功 {} 条", job, total, ok);
        }
    }
}
