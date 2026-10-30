package com.platform.cloud.mall.trade.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.mapper.row.DailySalesRow;
import com.platform.cloud.mall.trade.order.mapper.row.GoodsSalesRow;
import com.platform.cloud.mall.trade.order.mapper.row.SalesRow;
import com.platform.cloud.mall.trade.order.mapper.row.StatusCountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 Mapper。统计 SQL 全部用 #{} 绑定参数；「已支付」以 pay_time 非空为准，
 * 这样待支付时被取消的订单自然不计入，而支付后整单退款取消的订单仍计入当天销售（与支付渠道流水一致）。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT COALESCE(SUM(pay_amount), 0) AS sales, COUNT(*) AS orders FROM trd_order "
            + "WHERE deleted = 0 AND pay_time IS NOT NULL AND pay_time >= #{from} AND pay_time < #{to}")
    SalesRow sumPaid(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT DATE_FORMAT(pay_time, '%Y-%m-%d') AS date, COALESCE(SUM(pay_amount), 0) AS sales, "
            + "COUNT(*) AS orders FROM trd_order "
            + "WHERE deleted = 0 AND pay_time IS NOT NULL AND pay_time >= #{from} AND pay_time < #{to} "
            + "GROUP BY DATE_FORMAT(pay_time, '%Y-%m-%d')")
    List<DailySalesRow> dailyPaid(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT i.goods_id AS goodsId, MAX(i.goods_name) AS goodsName, MAX(i.pic) AS pic, "
            + "SUM(i.quantity) AS quantity, COALESCE(SUM(i.pay_amount), 0) AS amount "
            + "FROM trd_order_item i JOIN trd_order o ON o.id = i.order_id "
            + "WHERE o.deleted = 0 AND i.deleted = 0 AND o.pay_time IS NOT NULL AND o.pay_time >= #{from} "
            + "GROUP BY i.goods_id ORDER BY SUM(i.quantity) DESC, SUM(i.pay_amount) DESC LIMIT #{limit}")
    List<GoodsSalesRow> topGoods(@Param("from") LocalDateTime from, @Param("limit") int limit);

    @Select("SELECT status, COUNT(*) AS cnt FROM trd_order WHERE deleted = 0 GROUP BY status")
    List<StatusCountRow> countByStatus();

    /** 会员视角的状态计数：买家已删除的订单不计入角标 */
    @Select("SELECT status, COUNT(*) AS cnt FROM trd_order "
            + "WHERE deleted = 0 AND member_deleted = 0 AND member_id = #{memberId} GROUP BY status")
    List<StatusCountRow> countByStatusOfMember(@Param("memberId") Long memberId);
}
