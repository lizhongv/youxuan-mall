package com.platform.cloud.mall.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.cloud.mall.member.entity.MbrMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 会员 Mapper。账户类字段一律用「原值 + 增量」的条件更新，由数据库保证原子性：
 * 两个并发扣款各自读到 100 再写回 50 会丢一次扣款，而 {@code balance = balance + delta} 不会。
 */
@Mapper
public interface MbrMemberMapper extends BaseMapper<MbrMember> {

    /** 变更余额；扣减后为负时不更新，返回 0 */
    @Update("UPDATE mbr_member SET balance = balance + #{delta} "
            + "WHERE id = #{id} AND deleted = 0 AND balance + #{delta} >= 0")
    int changeBalance(@Param("id") Long id, @Param("delta") BigDecimal delta);

    /** 变更积分；扣减后为负时不更新，返回 0 */
    @Update("UPDATE mbr_member SET points = points + #{delta} "
            + "WHERE id = #{id} AND deleted = 0 AND points + #{delta} >= 0")
    int changePoints(@Param("id") Long id, @Param("delta") int delta);

    /** 锁定会员行读取账户值，回收奖励时计算「最多扣到 0」 */
    @Select("SELECT id, points, growth FROM mbr_member WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    MbrMember selectAccountForUpdate(@Param("id") Long id);

    /** 回收奖励：调用方已按当前值截断，条件 >= 再兜一次防止为负 */
    @Update("UPDATE mbr_member SET points = points - #{points}, growth = growth - #{growth} "
            + "WHERE id = #{id} AND deleted = 0 AND points >= #{points} AND growth >= #{growth}")
    int deductReward(@Param("id") Long id, @Param("points") int points, @Param("growth") int growth);

    /** 按注册日期分组计数（统计趋势一次查出全部日期） */
    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS d, COUNT(*) AS c FROM mbr_member "
            + "WHERE deleted = 0 AND created_at >= #{from} AND created_at < #{to} "
            + "GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')")
    List<Map<String, Object>> countRegisteredDaily(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 增加成长值（只增不减） */
    @Update("UPDATE mbr_member SET growth = growth + #{delta} WHERE id = #{id} AND deleted = 0")
    int addGrowth(@Param("id") Long id, @Param("delta") int delta);
}
