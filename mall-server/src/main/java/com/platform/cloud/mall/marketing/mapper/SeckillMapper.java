package com.platform.cloud.mall.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.cloud.mall.marketing.entity.Seckill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀活动。
 */
@Mapper
public interface SeckillMapper extends BaseMapper<Seckill> {

    /** 数据库侧最终防超卖：即使 Redis 数据丢失或被误改，sold 也不会超过活动库存 */
    @Update("UPDATE mkt_seckill SET sold = sold + #{quantity} "
            + "WHERE id = #{id} AND deleted = 0 AND sold + #{quantity} <= stock")
    int increaseSold(@Param("id") Long id, @Param("quantity") int quantity);

    @Update("UPDATE mkt_seckill SET sold = sold - #{quantity} WHERE id = #{id} AND sold >= #{quantity}")
    int decreaseSold(@Param("id") Long id, @Param("quantity") int quantity);
}
