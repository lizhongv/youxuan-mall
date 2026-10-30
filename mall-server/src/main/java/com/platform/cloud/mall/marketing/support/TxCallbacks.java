package com.platform.cloud.mall.marketing.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 把 Redis 这类不参与数据库事务的副作用挂到事务边界上：提交后再执行，或回滚时补偿。
 * 没有活动事务时立即执行（提交回调）或不执行（回滚回调）。
 */
@Slf4j
public final class TxCallbacks {

    private TxCallbacks() {
    }

    /** 事务提交后执行；回调异常只记录日志，不影响已提交的业务 */
    public static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.run();
                } catch (RuntimeException e) {
                    log.error("事务提交后回调执行失败", e);
                }
            }
        });
    }

    /** 事务回滚后执行补偿 */
    public static void afterRollback(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        action.run();
                    } catch (RuntimeException e) {
                        log.error("事务回滚补偿执行失败", e);
                    }
                }
            }
        });
    }
}
