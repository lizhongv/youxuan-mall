package com.platform.cloud.mall.marketing.job;

import com.platform.cloud.mall.marketing.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 应用启动后把未结束秒杀活动的剩余库存预热到 Redis。
 * 失败只记日志：抢购时发现键缺失还会按数据库懒加载，不能因为 Redis 暂时不可用就让应用起不来。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillWarmUpRunner {

    private final SeckillService seckillService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        try {
            int n = seckillService.warmUpAll();
            log.info("秒杀库存预热完成，活动数 {}", n);
        } catch (RuntimeException e) {
            log.warn("秒杀库存预热失败，将在抢购时懒加载: {}", e.getMessage());
        }
    }
}
