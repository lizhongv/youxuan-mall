package com.platform.cloud.mall.marketing.job;

import com.platform.cloud.mall.marketing.mapper.MemberCouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 每分钟把过期未用的会员券置为 EXPIRED。
 * 一条条件 UPDATE 完成，多实例部署时重复执行也无副作用（幂等），不需要分布式锁。
 * 查询接口本身也会把「未处理的过期券」按已过期展示，任务延迟不影响用户看到的结果。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponExpireJob {

    private final MemberCouponMapper memberCouponMapper;

    @Scheduled(fixedDelayString = "${mall.job.coupon-expire-delay-ms:60000}", initialDelay = 30_000)
    public void expire() {
        try {
            int n = memberCouponMapper.expire(LocalDateTime.now());
            if (n > 0) {
                log.info("过期优惠券 {} 张", n);
            }
        } catch (RuntimeException e) {
            log.error("优惠券过期任务失败", e);
        }
    }
}
