package com.platform.cloud.mall.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.cloud.mall.marketing.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券模板。计数字段全部用条件更新，不走「查出来 +1 再写回」，避免并发丢更新与超发。
 */
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 领取占用发行量：total = 0 表示不限量。影响行数 0 表示已领完或已停发。
     * 这条 UPDATE 同时持有模板行锁，同一张券的领取在事务内串行，后续「每人限领」检查因此可靠。
     */
    @Update("UPDATE mkt_coupon SET issued = issued + 1 "
            + "WHERE id = #{id} AND deleted = 0 AND status = 1 AND (total = 0 OR issued < total)")
    int increaseIssued(@Param("id") Long id);

    @Update("UPDATE mkt_coupon SET used = used + 1 WHERE id = #{id}")
    int increaseUsed(@Param("id") Long id);

    @Update("UPDATE mkt_coupon SET used = used - 1 WHERE id = #{id} AND used > 0")
    int decreaseUsed(@Param("id") Long id);
}
