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
 * 订单 Mapper。统计 SQL 全部用 #{} 绑定参数。
 * <p>
 * 销售口径：按支付时间归属日期，销售额 = 实付 − 已退款（trd_aftersale 中 REFUNDED 的金额）；
 * 支付后又整单退款取消的订单（status = CANCELLED 且 pay_time 非空）不计入销售额与订单数，
 * 否则后台会把退掉的钱也算成业绩。待支付时取消的订单 pay_time 为空，本来就不计入。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /** 单个订单已退款金额的相关子查询，走 trd_aftersale(order_id) 索引 */
    String REFUNDED_OF_ORDER = "COALESCE((SELECT SUM(a.amount) FROM trd_aftersale a "
            + "WHERE a.order_id = o.id AND a.deleted = 0 AND a.status = 'REFUNDED'), 0)";

    /** 已支付且未因全额退款取消的订单 */
    String PAID_NOT_REFUND_CANCELLED = "o.deleted = 0 AND o.pay_time IS NOT NULL AND o.status <> 'CANCELLED' ";

    @Select("SELECT COALESCE(SUM(o.pay_amount - " + REFUNDED_OF_ORDER + "), 0) AS sales, COUNT(*) AS orders "
            + "FROM trd_order o WHERE " + PAID_NOT_REFUND_CANCELLED
            + "AND o.pay_time >= #{from} AND o.pay_time < #{to}")
    SalesRow sumPaid(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 一次 GROUP BY 查出区间内每天的数据（没有订单的日期不返回，由调用方补 0） */
    @Select("SELECT DATE_FORMAT(o.pay_time, '%Y-%m-%d') AS date, "
            + "COALESCE(SUM(o.pay_amount - " + REFUNDED_OF_ORDER + "), 0) AS sales, COUNT(*) AS orders "
            + "FROM trd_order o WHERE " + PAID_NOT_REFUND_CANCELLED
            + "AND o.pay_time >= #{from} AND o.pay_time < #{to} "
            + "GROUP BY DATE_FORMAT(o.pay_time, '%Y-%m-%d')")
    List<DailySalesRow> dailyPaid(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * 热销商品：金额扣除明细已退款；已全额退款的明细不计销量。
     * 退款子表按 refund_time ≥ from 过滤：退款一定发生在支付之后，可大幅缩小派生表。
     */
    @Select("SELECT i.goods_id AS goodsId, MAX(i.goods_name) AS goodsName, MAX(i.pic) AS pic, "
            + "SUM(CASE WHEN COALESCE(r.refunded, 0) >= i.pay_amount AND r.refunded IS NOT NULL THEN 0 "
            + "ELSE i.quantity END) AS quantity, "
            + "COALESCE(SUM(i.pay_amount - COALESCE(r.refunded, 0)), 0) AS amount "
            + "FROM trd_order_item i JOIN trd_order o ON o.id = i.order_id "
            + "LEFT JOIN (SELECT order_item_id, SUM(amount) AS refunded FROM trd_aftersale "
            + "WHERE deleted = 0 AND status = 'REFUNDED' AND refund_time >= #{from} GROUP BY order_item_id) r "
            + "ON r.order_item_id = i.id "
            + "WHERE i.deleted = 0 AND " + PAID_NOT_REFUND_CANCELLED + "AND o.pay_time >= #{from} "
            + "GROUP BY i.goods_id HAVING quantity > 0 ORDER BY quantity DESC, amount DESC LIMIT #{limit}")
    List<GoodsSalesRow> topGoods(@Param("from") LocalDateTime from, @Param("limit") int limit);

    /**
     * 锁定订单行（SELECT ... FOR UPDATE），用于发货与售后申请互斥：二者都先锁订单行，保证串行执行，
     * 不会出现「发货检查时还没有售后、刚发完货售后就申请成功」的交错。必须在事务内调用。
     */
    @Select("SELECT * FROM trd_order WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Order selectByIdForUpdate(@Param("id") Long id);

    /** 会员在某秒杀活动已下单（未取消）的件数：Redis 个人已购计数丢失时以订单为准兜底限购 */
    @Select("SELECT COALESCE(SUM(i.quantity), 0) FROM trd_order o "
            + "JOIN trd_order_item i ON i.order_id = o.id AND i.deleted = 0 "
            + "WHERE o.deleted = 0 AND o.seckill_id = #{seckillId} AND o.member_id = #{memberId} "
            + "AND o.status <> 'CANCELLED'")
    int sumSeckillPurchased(@Param("seckillId") Long seckillId, @Param("memberId") Long memberId);

    @Select("SELECT status, COUNT(*) AS cnt FROM trd_order WHERE deleted = 0 GROUP BY status")
    List<StatusCountRow> countByStatus();

    /** 会员视角的状态计数：买家已删除的订单不计入角标 */
    @Select("SELECT status, COUNT(*) AS cnt FROM trd_order "
            + "WHERE deleted = 0 AND member_deleted = 0 AND member_id = #{memberId} GROUP BY status")
    List<StatusCountRow> countByStatusOfMember(@Param("memberId") Long memberId);
}
