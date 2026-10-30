package com.platform.cloud.mall.trade.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务钩子：把 Redis 这类不受数据库事务控制的操作，和数据库事务的结果对齐。
 * <p>
 * 典型场景是秒杀名额：名额在 Redis 里扣，订单在 MySQL 里写，订单事务回滚时必须把名额还回去，
 * 而关单时则要等事务提交后才还，避免关单失败却已经放出名额导致超卖。
 */
@Slf4j
public final class TxHooks {

    private TxHooks() {
    }

    /** 当前事务回滚后执行；无事务时不执行（调用方没有事务就不存在「回滚」） */
    public static void afterRollback(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    runQuietly(action);
                }
            }
        });
    }

    /** 当前事务提交后执行；无事务时立即执行 */
    public static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runQuietly(action);
            }
        });
    }

    /** 事务已结束，异常无法再回滚任何东西，只能记录日志供人工补偿 */
    private static void runQuietly(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.error("事务后置动作执行失败，需人工核对", e);
        }
    }
}
