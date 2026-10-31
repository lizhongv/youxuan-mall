package com.platform.cloud.goods.seckill;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import com.platform.cloud.bootstrap.goods.spu.entity.SkuEntity;
import com.platform.cloud.bootstrap.goods.spu.mapper.SkuMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Phase 4 演示：Redis Lua 原子扣减秒杀库存。
 * <p>对比 Phase 2.6 StockController（DB 乐观锁）：本服务无 DB 压力 + 严格无超卖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillService {

    public static final String STOCK_KEY = "seckill:stock:";
    public static final String SOLD_KEY  = "seckill:sold:";

    private final StringRedisTemplate redis;
    private final SkuMapper skuMapper;

    private RedisScript<Long> deductScript;

    @PostConstruct
    public void init() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/seckill_deduct.lua")));
        script.setResultType(Long.class);
        this.deductScript = script;
    }

    /** 把 DB 中 SKU 的当前 stock 预热到 Redis */
    public int preload(Long skuId) {
        SkuEntity sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "SKU 不存在: " + skuId);
        }
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        redis.opsForValue().set(STOCK_KEY + skuId, String.valueOf(stock));
        redis.opsForValue().set(SOLD_KEY + skuId, "0");
        log.info("Seckill preload: skuId={} stock={}", skuId, stock);
        return stock;
    }

    /** Redis Lua 原子扣 + 异步落库（这里同步落库 demo；真实场景 MQ 削峰）*/
    public long buy(Long skuId, int quantity) {
        Long ret = redis.execute(deductScript,
                List.of(STOCK_KEY + skuId),
                String.valueOf(quantity));
        if (ret == null || ret == -1L) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动未开始或商品未上架");
        }
        if (ret == -2L) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "已售罄");
        }
        // 异步累加销量计数（统计用）
        redis.opsForValue().increment(SOLD_KEY + skuId, quantity);
        return ret;
    }

    /** 真实场景应通过 MQ 异步把 Redis 扣减结果落 DB；这里同步演示 */
    public void persistToDb(Long skuId, int quantity) {
        skuMapper.update(null,
                new LambdaUpdateWrapper<SkuEntity>()
                        .eq(SkuEntity::getId, skuId)
                        .ge(SkuEntity::getStock, quantity)
                        .setSql("stock = stock - " + quantity));
    }

    public Long currentStock(Long skuId) {
        String v = redis.opsForValue().get(STOCK_KEY + skuId);
        return v == null ? null : Long.parseLong(v);
    }

    public Long currentSold(Long skuId) {
        String v = redis.opsForValue().get(SOLD_KEY + skuId);
        return v == null ? 0L : Long.parseLong(v);
    }
}
