package com.platform.cloud.mall.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.cloud.mall.marketing.entity.MemberCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 会员券。状态流转全部用「WHERE 原状态」的条件更新，同一张券不会被两个订单同时核销。
 */
@Mapper
public interface MemberCouponMapper extends BaseMapper<MemberCoupon> {

    /**
     * 当前读统计某会员已领某券的数量（含已用、已过期，限领按「领过」算）。
     * FOR UPDATE 读的是最新已提交数据，配合先执行的 increaseIssued 行锁，避免同一会员并发领取超出限领。
     */
    @Select("SELECT COUNT(*) FROM mkt_member_coupon "
            + "WHERE coupon_id = #{couponId} AND member_id = #{memberId} AND deleted = 0 FOR UPDATE")
    long countReceivedForUpdate(@Param("couponId") Long couponId, @Param("memberId") Long memberId);

    /** 核销：只有本人的、未使用且在有效期内的券才能核销 */
    @Update("UPDATE mkt_member_coupon SET status = 'USED', used_at = #{now}, order_id = #{orderId}, "
            + "version = version + 1 "
            + "WHERE id = #{id} AND member_id = #{memberId} AND status = 'UNUSED' AND deleted = 0 "
            + "AND start_at <= #{now} AND end_at > #{now}")
    int use(@Param("id") Long id, @Param("memberId") Long memberId, @Param("orderId") Long orderId,
            @Param("now") LocalDateTime now);

    /** 退还：已过有效期的直接置为 EXPIRED，否则回到 UNUSED */
    @Update("UPDATE mkt_member_coupon SET status = CASE WHEN end_at <= #{now} THEN 'EXPIRED' ELSE 'UNUSED' END, "
            + "used_at = NULL, order_id = NULL, version = version + 1 "
            + "WHERE id = #{id} AND status = 'USED' AND deleted = 0")
    int giveBack(@Param("id") Long id, @Param("now") LocalDateTime now);

    /** 定时任务：把过期未用的券置为 EXPIRED */
    @Update("UPDATE mkt_member_coupon SET status = 'EXPIRED', version = version + 1 "
            + "WHERE status = 'UNUSED' AND end_at <= #{now} AND deleted = 0")
    int expire(@Param("now") LocalDateTime now);
}
