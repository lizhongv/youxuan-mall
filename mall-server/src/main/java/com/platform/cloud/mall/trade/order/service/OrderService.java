package com.platform.cloud.mall.trade.order.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.trade.order.dto.AdminOrderPageQuery;
import com.platform.cloud.mall.trade.order.dto.OrderDeliverRequest;
import com.platform.cloud.mall.trade.order.dto.OrderPageQuery;
import com.platform.cloud.mall.trade.order.vo.OrderVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单查询与状态操作（下单见 OrderCheckoutService，支付见 PayService）。
 * 会员侧方法都带 memberId，查询条件强制 member_id = 当前用户。
 */
public interface OrderService {

    PageVO<OrderVO> memberPage(Long memberId, OrderPageQuery query);

    OrderVO memberDetail(Long memberId, Long orderId);

    void memberCancel(Long memberId, Long orderId, String reason);

    void memberReceive(Long memberId, Long orderId);

    void memberDelete(Long memberId, Long orderId);

    PageVO<OrderVO> adminPage(AdminOrderPageQuery query);

    OrderVO adminDetail(Long orderId);

    Map<String, Long> statusCount();

    void deliver(Long adminId, Long orderId, OrderDeliverRequest req);

    void adjustPrice(Long adminId, Long orderId, BigDecimal adjustAmount);

    void updateAdminRemark(Long adminId, Long orderId, String adminRemark);

    void adminCancel(Long adminId, Long orderId, String reason);

    /** 定时任务：找出已超时未支付的订单 ID（按 id 升序，afterId 为游标，首页传 null） */
    List<Long> findExpiredUnpaid(Long afterId, int limit);

    /** 定时任务：关闭单个超时订单（独立事务，单个失败不影响其他） */
    void closeExpired(Long orderId);

    /** 定时任务：找出已到自动收货时间、且没有进行中售后的订单 ID（按 id 升序游标分页） */
    List<Long> findAutoReceivable(Long afterId, int limit);

    /** 定时任务：自动确认收货单个订单 */
    void autoReceive(Long orderId);
}
