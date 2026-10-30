package com.platform.cloud.mall.trade.job;

import com.platform.cloud.mall.trade.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 交易定时任务（每分钟）：关闭超时未支付订单、自动确认收货。
 * <p>
 * 每个订单单独一个事务（调用 OrderService 的事务方法），某个订单失败只记日志，不影响同批其他订单；
 * 每轮最多处理 {@link #BATCH} 条，积压时下一分钟继续，避免单次任务过长。
 * 过期优惠券置为 EXPIRED 属于营销领域的任务，不在这里处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeScheduler {

    static final int BATCH = 200;
    static final String JOB_CLOSE = "close-expired-order";
    static final String JOB_RECEIVE = "auto-receive-order";

    private final OrderService orderService;
    private final TradeJobLock lock;

    @Scheduled(cron = "0 * * * * ?")
    public void closeExpiredOrders() {
        if (!lock.tryLock(JOB_CLOSE)) {
            return;
        }
        process(JOB_CLOSE, orderService.findExpiredUnpaid(BATCH), orderService::closeExpired);
    }

    @Scheduled(cron = "30 * * * * ?")
    public void autoReceiveOrders() {
        if (!lock.tryLock(JOB_RECEIVE)) {
            return;
        }
        process(JOB_RECEIVE, orderService.findAutoReceivable(BATCH), orderService::autoReceive);
    }

    private void process(String job, List<Long> ids, Consumer<Long> action) {
        int ok = 0;
        for (Long id : ids) {
            try {
                action.accept(id);
                ok++;
            } catch (RuntimeException e) {
                log.warn("{} 处理订单失败 orderId={}", job, id, e);
            }
        }
        if (!ids.isEmpty()) {
            log.info("{} 本轮处理 {} 条，成功 {} 条", job, ids.size(), ok);
        }
    }
}
