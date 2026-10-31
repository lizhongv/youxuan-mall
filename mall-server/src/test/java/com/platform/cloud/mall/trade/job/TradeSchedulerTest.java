package com.platform.cloud.mall.trade.job;

import com.platform.cloud.mall.trade.order.service.OrderService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 定时任务：id 游标翻页、单条失败不影响后续、单轮上限。
 */
class TradeSchedulerTest {

    private final OrderService orderService = mock(OrderService.class);
    private final TradeJobLock lock = mock(TradeJobLock.class);
    private final TradeScheduler scheduler = new TradeScheduler(orderService, lock);

    private static List<Long> ids(long from, long to) {
        return LongStream.rangeClosed(from, to).boxed().toList();
    }

    @Test
    void 按id游标翻页直到取完_单条失败不影响后续() {
        when(lock.tryLock(TradeScheduler.JOB_CLOSE)).thenReturn(true);
        when(orderService.findExpiredUnpaid(isNull(), anyInt())).thenReturn(ids(1, TradeScheduler.BATCH));
        when(orderService.findExpiredUnpaid(eq((long) TradeScheduler.BATCH), anyInt())).thenReturn(List.of(901L, 902L));
        doThrow(new IllegalStateException("boom")).when(orderService).closeExpired(1L);

        scheduler.closeExpiredOrders();

        // 第一条失败，后面照常处理；第二页从上一页最后一个 id 之后继续，不足一页即结束
        verify(orderService).closeExpired(2L);
        verify(orderService).closeExpired(902L);
        verify(orderService, times(TradeScheduler.BATCH + 2)).closeExpired(anyLong());
        verify(orderService, times(2)).findExpiredUnpaid(any(), anyInt());
    }

    @Test
    void 达到单轮上限即停止() {
        when(lock.tryLock(TradeScheduler.JOB_RECEIVE)).thenReturn(true);
        List<Long> seen = new ArrayList<>();
        when(orderService.findAutoReceivable(any(), anyInt())).thenAnswer(inv -> {
            Long after = inv.getArgument(0);
            int limit = inv.getArgument(1);
            long start = after == null ? 1 : after + 1;
            seen.add(start);
            return ids(start, start + limit - 1);
        });

        scheduler.autoReceiveOrders();

        verify(orderService, times(TradeScheduler.MAX_PER_ROUND)).autoReceive(anyLong());
        assertThat(seen).hasSize(TradeScheduler.MAX_PER_ROUND / TradeScheduler.BATCH);
    }

    @Test
    void 未拿到锁不执行() {
        when(lock.tryLock(TradeScheduler.JOB_CLOSE)).thenReturn(false);
        scheduler.closeExpiredOrders();
        verify(orderService, never()).findExpiredUnpaid(any(), anyInt());
    }

    private static Long any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
