package com.platform.cloud.mall.system.support;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 日志异步写入。
 * <p>
 * 刻意不注册成 {@code Executor} 类型的 Bean：那样会让 Spring Boot 不再自动配置 applicationTaskExecutor，
 * 影响其他领域的异步任务。独立的小线程池保证日志堆积时不拖垮业务线程；队列满时丢弃并告警，
 * 审计日志重要但不能反过来阻塞后台操作。
 */
@Slf4j
@Component
public class AsyncLogWriter {

    private final ThreadPoolTaskExecutor pool;

    public AsyncLogWriter() {
        pool = new ThreadPoolTaskExecutor();
        // 后台操作量不大，2 个线程足够；突发时最多 4 个，队列 2000 条可缓冲数据库短暂抖动
        pool.setCorePoolSize(2);
        pool.setMaxPoolSize(4);
        pool.setQueueCapacity(2000);
        pool.setThreadNamePrefix("sys-log-");
        pool.setRejectedExecutionHandler((r, e) -> log.warn("日志队列已满，丢弃一条日志"));
        // 优雅停机时把队列里的日志写完
        pool.setWaitForTasksToCompleteOnShutdown(true);
        pool.setAwaitTerminationSeconds(10);
        pool.initialize();
    }

    public void submit(Runnable task) {
        pool.execute(task);
    }

    @PreDestroy
    public void shutdown() {
        pool.shutdown();
    }
}
